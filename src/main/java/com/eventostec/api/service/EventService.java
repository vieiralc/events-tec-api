package com.eventostec.api.service;

import com.amazonaws.services.s3.AmazonS3;
import com.eventostec.api.domain.address.Address;
import com.eventostec.api.domain.event.Event;
import com.eventostec.api.domain.event.EventRequestDTO;
import com.eventostec.api.domain.event.EventResponseDTO;
import com.eventostec.api.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EventService {
    @Value("${aws.bucket.name}")
    private String bucketName;
    @Autowired
    private AmazonS3 s3Client;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private AddressService addressService;

    public Event createEvent(EventRequestDTO data) {
        String imgUrl = null;

        if (data.image() != null) {
            imgUrl = this.uploadImage(data.image());
        }

        Event newEvent = new Event();
        newEvent.setTitle(data.title());
        newEvent.setDescription(data.description());
        newEvent.setEventUrl(data.eventUrl());
        newEvent.setDate(new Date(data.date()));
        newEvent.setImgUrl(imgUrl);
        newEvent.setRemote(data.remote());
        eventRepository.save(newEvent);

        if (!data.remote()) {
            this.addressService.createAddress(data, newEvent);
        }

        return newEvent;
    }

    private String uploadImage(MultipartFile multipartFile) {
        String fileName = this.generateUniqueImgName(multipartFile.getOriginalFilename());

        try {
            File file = this.convertMultiPartToFile(multipartFile);
            s3Client.putObject(bucketName, fileName, file);
            file.delete();
            return  s3Client.getUrl(bucketName, fileName).toString();
        } catch (Exception e) {
            System.out.println("error uploading file: " + e);
            return "";
        }
    }

    private String generateUniqueImgName(String originalFileName) {
        return UUID.randomUUID() + " " + originalFileName;
    }

    private File convertMultiPartToFile(MultipartFile file) throws Exception {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    public List<EventResponseDTO> getUpcomingEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventsPage = this.eventRepository.findUpcomingEvents(new Date(), pageable);
        return eventsPage.map(event -> new EventResponseDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDate(),
                event.getAddress() != null ? event.getAddress().getCity() : "",
                event.getAddress() != null ? event.getAddress().getUf() : "",
                event.getRemote(),
                event.getEventUrl(),
                event.getImgUrl())
        ).stream().toList();
    }

    public List<EventResponseDTO> getFilteredEvents(int page, int size, String title, String city, String uf,
                                                    Date startDate, Date endDate) {
        // validate input
        title = (title != null) ? title : "";
        city = (city != null) ? city : "";
        uf = (uf != null) ? uf : "";
        startDate = (startDate != null) ? startDate : new Date();
        if (endDate == null || endDate.before(startDate)) {
            LocalDate currentDate = LocalDate.now();
            LocalDate tenYearsFromNow = currentDate.plusYears(10);
            endDate = Date.from(tenYearsFromNow.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventsPage = this.eventRepository.findFilteredEvents(title, city, uf, startDate,
                endDate, pageable);
        return eventsPage.map(event -> new EventResponseDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDate(),
                event.getAddress() != null ? event.getAddress().getCity() : "",
                event.getAddress() != null ? event.getAddress().getUf() : "",
                event.getRemote(),
                event.getEventUrl(),
                event.getImgUrl())
        ).stream().toList();
    }
}
