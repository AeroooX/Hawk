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

package me.islandscout.hawk.check.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class AimbotConvergence extends CustomCheck {

    //You better turn off your aimbot when your opponent is standing still!

    private Map<UUID, Vector> lastConvergencePointMap;

    public AimbotConvergence() {
        super("aimbotdistribution", true, -1, 0, 0.999, 5000, "%player% is using aimbot (convergence), VL: %vl%", null);
        this.lastConvergencePointMap = new HashMap<>();
    }

    @Override
    protected void check(Event event) {
        if(event instanceof MoveEvent) {
            MoveEvent e = (MoveEvent) event;
            if(!e.hasDeltaPos() || e.hasTeleported())
                return;
            HawkPlayer pp = e.getHawkPlayer();
            UUID uuid = pp.getUuid();
            Vector prePos = e.getFrom().toVector().clone().subtract(pp.getVelocity()).add(new Vector(0, 1.62, 0));
            Vector postPos = e.getFrom().toVector().add(new Vector(0, 1.62, 0));
            Vector preDir = e.getFrom().getDirection();
            Vector postDir = e.getTo().getDirection();
            Pair<Vector, Vector> points = new Ray(prePos, preDir).closestPointsBetweenLines(new Ray(postPos, postDir));

            Vector lastConvergence = lastConvergencePointMap.get(uuid);
            Vector convergence = points.getKey().add(points.getValue()).multiply(0.5);

            if(lastConvergence != null) {
                double distance = lastConvergence.distanceSquared(convergence);
                if(!Double.isNaN(distance)) {

                    //Debug.broadcastMessage( distance + "");

                    if(distance < 0.00000001) //one VL deserves an autoban
                        punish(pp, false, e);
                    else
                        reward(pp);

                }
            }

            lastConvergencePointMap.put(uuid, convergence);
        }
    }

    @Override
    public void removeData(Player p) {
        lastConvergencePointMap.remove(p.getUniqueId());
    }
}