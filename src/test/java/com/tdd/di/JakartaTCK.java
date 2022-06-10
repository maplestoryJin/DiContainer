package com.tdd.di;

import jakarta.inject.Named;
import junit.framework.Test;
import org.atinject.tck.Tck;
import org.atinject.tck.auto.*;
import org.atinject.tck.auto.accessories.Cupholder;
import org.atinject.tck.auto.accessories.SpareTire;

public class JakartaTCK {

    public static Test suite() {
        ContextConfig config = new ContextConfig();
        config.from(new Config() {
            @Export(Car.class)
            Convertible car;

            @Drivers
            @Export(Seat.class)
            DriversSeat drivers;

            Seat seat;

            Tire tire;

            @Export(Engine.class)
            V8Engine engine;

            @Named("spare")
            @Export(Tire.class)
            SpareTire spare;

            FuelTank fuelTank;

            @Static
            SpareTire spareTire;
            Cupholder cupholder;

            @Static
            Convertible convertible;


        });

        Car car = config.getContext().get(ComponentRef.of(Car.class)).get();
        return Tck.testsFor(car, true, true);
    }


}
