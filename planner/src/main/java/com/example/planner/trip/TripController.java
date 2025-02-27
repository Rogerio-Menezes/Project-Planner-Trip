package com.example.planner.trip;

import com.example.planner.activity.*;
import com.example.planner.link.LinkData;
import com.example.planner.link.LinkRequestPayload;
import com.example.planner.link.LinkResponse;
import com.example.planner.link.LinkService;
import com.example.planner.participant.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private TripRepository repository;

    @Autowired
    private LinkService linkService;


    @Autowired
    private ActivityService activityService;

    @PostMapping
    public ResponseEntity<TripCreateResponse> createTrip(@RequestBody TripRequestPayload payload) {
        Trip newTrip = new Trip(payload);

        this.repository.save(newTrip);
        this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip);

        return ResponseEntity.ok(new TripCreateResponse(newTrip.getId()));

    }
    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripDetails(@PathVariable UUID id){
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()){
            return ResponseEntity.ok().body(repository.getReferenceById(id));
        }
            return ResponseEntity.notFound().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID id, @RequestBody TripRequestPayload payload ){
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()){
            Trip rawTrip = trip.get();
            rawTrip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setDestination(payload.destination());
            this.repository.save(rawTrip);
            return ResponseEntity.ok(rawTrip);
        }
        return  ResponseEntity.notFound().build();
    }
    @GetMapping("/{id}/confirm")
    public ResponseEntity<Trip> confirmTrip(@PathVariable UUID id){
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()){
            Trip rawTrip = trip.get();
            rawTrip.setIsConfirmed(true);
            this.repository.save(rawTrip);
            this.participantService.triggerConfirmationEmailToParticipants(id);
            return ResponseEntity.ok(rawTrip);

        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<ParticipantCreateResponse> inviteParticipants(@PathVariable UUID id, @RequestBody ParticipantRequestPayload payload){
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()){
            Trip rawTrip = trip.get();

            ParticipantCreateResponse participantResponse = this.participantService.registerParticipantToEvent(payload.email(), rawTrip);
            if (rawTrip.getIsConfirmed()) this.participantService.triggerConfirmationEmailToParticipant( payload.email());
            return ResponseEntity.ok(participantResponse);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/participants")
     public ResponseEntity<List<ParticipantsData>> getAllParticipants(@PathVariable UUID id){
        List<ParticipantsData> participantList = this.participantService.getAllParticipantsFromEvent(id);

        return ResponseEntity.ok(participantList);
    }

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityResponse> registerActivity(@PathVariable UUID id, @RequestBody ActivityRequestPayload payload){
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()){
            Trip rawTrip = trip.get();
            ActivityResponse activityResponse = this.activityService.registerActivity(payload, rawTrip);

            return ResponseEntity.ok(activityResponse);
        }
        return ResponseEntity.notFound().build();
    }
    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityData>> getAllActivities(@PathVariable UUID id){
        List<ActivityData> activityDataList = this.activityService.getAllActivitiesFromId(id);
        return ResponseEntity.ok(activityDataList);
    }

    @PostMapping("/{id}/links")
    public ResponseEntity<LinkResponse> registerLink(@PathVariable UUID id, @RequestBody LinkRequestPayload payload){
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            LinkResponse linkResponse = this.linkService.registerLinks(payload, rawTrip);

            return ResponseEntity.ok(linkResponse);
        }
        return  ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/links")
    public ResponseEntity<List<LinkData>> getAllLinks(@PathVariable UUID id){
        List<LinkData> linkDataList = this.linkService.getAllLinksFromTrip(id);
        return ResponseEntity.ok(linkDataList);
    }

}
