/*
 * This file is part of Hawk Anticheat.
 * Copyright (C) 2018 Hawk Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.AdjacentBlocks;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Inertia extends MovementCheck {

    //"Inertia is a property of matter... Bill, Bill, Bill..."

    private Map<UUID, Long> lastTickOnGround;

    public Inertia() {
        super("inertia", true, -1, 5, 0.995, 5000, "%player% failed inertia, VL: %vl%", null);
        lastTickOnGround = new HashMap<>();
    }

    @Override
    public void check(MoveEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Vector moveVector = new Vector(e.getTo().getX() - e.getFrom().getX(), 0, e.getTo().getZ() - e.getFrom().getZ());
        Vector prevVector = pp.getVelocity().clone().setY(0);
        double horizSpeedSquared = Math.pow(e.getTo().getX() - e.getFrom().getX(), 2) + Math.pow(e.getTo().getZ() - e.getFrom().getZ(), 2);
        double deltaAngle = moveVector.angle(prevVector);
        boolean onGround = e.isOnGround(); //um... is this safe?
        boolean wasOnGround = pp.isOnGround(); //um... is this safe?
        if(onGround)
            lastTickOnGround.put(p.getUniqueId(), pp.getCurrentTick());
        long ticksSinceGround = pp.getCurrentTick() - lastTickOnGround.getOrDefault(p.getUniqueId(), -1L);

        if (!AdjacentBlocks.blockNearbyIsSolid(e.getTo(), true) && !wasOnGround && !onGround && !e.hasAcceptedKnockback() && !e.isTouchingBlocks() &&
                !AdjacentBlocks.blockNearbyIsSolid(e.getTo().clone().add(0, 1, 0), true) && !p.isFlying() && !p.isInsideVehicle()) {

            double magnitudeThres;
            double prevSpeed = e.hasHitSlowdown() ? prevVector.length() * 0.6 : prevVector.length();
            if(AdjacentBlocks.blockAdjacentIsLiquid(e.getFrom()) || AdjacentBlocks.blockAdjacentIsLiquid(e.getFrom().clone().add(0, 1, 0))) {
                magnitudeThres = 0; //screw it
            }
            else if(ticksSinceGround == 2) {
                magnitudeThres = 0.546 * prevSpeed - 0.026001;
            } else {
                magnitudeThres = 0.91 * prevSpeed - 0.026001;
            }

            //angle check
            if (horizSpeedSquared > 0.05 && deltaAngle > 0.2) {
                punishAndTryRubberband(pp, e, p.getLocation());

            //magnitude check
            } else if(prevVector.lengthSquared() > 0.01 && moveVector.length() < magnitudeThres) {
                punishAndTryRubberband(pp, e, p.getLocation());
            }

            else {
                reward(pp);
            }
        }
    }
}
