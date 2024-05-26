package es.iesmm.proyecto.drivehub.backend.controller.trip;

import es.iesmm.proyecto.drivehub.backend.model.http.request.trip.TripDraftRequest;
import es.iesmm.proyecto.drivehub.backend.model.trip.TripModel;
import es.iesmm.proyecto.drivehub.backend.model.trip.draft.TripDraftModel;
import es.iesmm.proyecto.drivehub.backend.model.user.UserModel;
import es.iesmm.proyecto.drivehub.backend.service.trip.TripService;
import es.iesmm.proyecto.drivehub.backend.service.user.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

@RestController
@RequestMapping("/trip")
@AllArgsConstructor
public class TripController {

    private final TripService tripService;
    private final UserService userService;

    @PostMapping("/draft")
    @ResponseBody
    public ResponseEntity<TripDraftModel> createDraft(@AuthenticationPrincipal UserDetails userDetails, @RequestBody TripDraftRequest request) {
        UserModel user = (UserModel) userDetails;
        try {
            return ResponseEntity.ok(tripService.createDraft(user, request.origin(), request.destination()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage())).build();
        }
    }

    @GetMapping("/draft/{id}")
    @ResponseBody
    public ResponseEntity<TripDraftModel> getDraft(@PathVariable String id) {
        return tripService.findDraft(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/start/{draftId}")
    @ResponseBody
    public ResponseEntity<TripModel> startTrip(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String draftId, @RequestParam(required = false, defaultValue = "false") boolean sendPackage) {
        UserModel user = (UserModel) userDetails;
        try {
            Optional<TripDraftModel> tripDraftModel = tripService.findDraft(draftId);

            return tripDraftModel
                    .map(draftModel -> ResponseEntity.ok(tripService.createTrip(user, draftModel, sendPackage)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage())).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage())).build();
        }
    }

    @GetMapping("/active")
    @ResponseBody
    public ResponseEntity<TripModel> getActiveTrip(@AuthenticationPrincipal UserDetails userDetails) {
        UserModel user = (UserModel) userDetails;
        return tripService.findActiveByPassenger(user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/active/cancel")
    @ResponseBody
    public ResponseEntity<Object> cancelActiveTrip(@AuthenticationPrincipal UserDetails userDetails) {
        UserModel user = (UserModel) userDetails;
        return tripService.findActiveByPassenger(user.getId())
                .map(tripModel -> {
                    tripService.cancelTrip(tripModel);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/driver/active")
    @ResponseBody
    @PreAuthorize("hasRole('DRIVER_CHAUFFEUR')")
    public ResponseEntity<TripModel> getActiveTripForDriver(@AuthenticationPrincipal UserDetails userDetails) {
        UserModel user = (UserModel) userDetails;
        return tripService.findActiveByDriver(user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/driver/active/finish")
    @ResponseBody
    @PreAuthorize("hasRole('DRIVER_CHAUFFEUR')")
    public ResponseEntity<Object> finishActiveTripForDriver(@AuthenticationPrincipal UserDetails userDetails, @RequestParam(required = false, defaultValue = "false") boolean cancel) {
        UserModel user = (UserModel) userDetails;
        return tripService.findActiveByDriver(user.getId())
                .map(tripModel -> {
                    if (cancel) { // Cancelled trips are not paid
                        tripService.cancelTrip(tripModel);
                    } else {
                        tripService.finishTrip(tripModel);
                    }
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /*
     * Actualizar el estado del viaje en el fronend, cuando hay un cambio en el estado
     * Los estados que se van a actualizar son los siguientes: cuando el conductor acepta el viaje
     * y cuando el conductor llega al destino.
     */
    @PostMapping("/stream/status")
    public ResponseEntity<SseEmitter> streamTripStatus(@AuthenticationPrincipal UserDetails userDetails) {
        UserModel user = (UserModel) userDetails;

        return tripService.findActiveByPassenger(user.getId())
                .map(trip -> ResponseEntity.ok(tripService.streamTripStatus(trip)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     * Actualizar la ubicación del conductor en el frontend, cuando el conductor se mueve para mostrarlo
     * en el mapa.
     */
    @PostMapping("/stream/location")
    public ResponseEntity<SseEmitter> streamLocationTrips(@AuthenticationPrincipal UserDetails userDetails) {
        UserModel user = (UserModel) userDetails;

        return tripService.findActiveByDriver(user.getId())
                .map(trip -> ResponseEntity.ok(tripService.streamTripLocation(trip)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     * Los conductores se subscriben a este endpoint para recibir notificaciones de nuevos viajes que
     * se han creado y que están disponibles para ser aceptados, cuando se subscriben se toman como
     * disponibles para aceptar viajes. Cuando cierran la conexión, se toman como no disponibles.
     */
    @PostMapping("/stream/duty")
    @PreAuthorize("hasRole('DRIVER_CHAUFFEUR')")
    public SseEmitter streamDuty(@AuthenticationPrincipal UserDetails userDetails) {
        return tripService.streamDuty((UserModel) userDetails);
    }

}
