package es.iesmm.proyecto.drivehub.backend.service.vehicle;

import es.iesmm.proyecto.drivehub.backend.model.rent.vehicle.RentCar;

import java.util.List;
import java.util.Optional;

public interface VehicleService {
    List<RentCar> findAvailableVehicles();

    List<RentCar> findAll();

    Optional<RentCar> findById(Long vehicleId);
}
