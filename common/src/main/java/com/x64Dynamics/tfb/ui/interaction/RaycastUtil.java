package com.x64Dynamics.tfb.ui.interaction;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3d;

public final class RaycastUtil {
    private RaycastUtil() {}

    public static Double intersectRayBox(Vec3d rayOrigin, Vec3d rayDir, BoundingBox3i box) {
        double minX = box.min().x();
        double maxX = box.max().x() + 1.0;
        double minY = box.min().y();
        double maxY = box.max().y() + 1.0;
        double minZ = box.min().z();
        double maxZ = box.max().z() + 1.0;

        double tmin = (minX - rayOrigin.x()) / (rayDir.x() == 0 ? 1e-6 : rayDir.x());
        double tmax = (maxX - rayOrigin.x()) / (rayDir.x() == 0 ? 1e-6 : rayDir.x());
        if (tmin > tmax) { double tmp = tmin; tmin = tmax; tmax = tmp; }

        double tymin = (minY - rayOrigin.y()) / (rayDir.y() == 0 ? 1e-6 : rayDir.y());
        double tymax = (maxY - rayOrigin.y()) / (rayDir.y() == 0 ? 1e-6 : rayDir.y());
        if (tymin > tymax) { double tmp = tymin; tymin = tymax; tymax = tmp; }

        if (tmin > tymax || tymin > tmax) return null;
        if (tymin > tmin) tmin = tymin;
        if (tymax < tmax) tmax = tymax;

        double tzmin = (minZ - rayOrigin.z()) / (rayDir.z() == 0 ? 1e-6 : rayDir.z());
        double tzmax = (maxZ - rayOrigin.z()) / (rayDir.z() == 0 ? 1e-6 : rayDir.z());
        if (tzmin > tzmax) { double tmp = tzmin; tzmin = tzmax; tzmax = tmp; }

        if (tmin > tzmax || tzmin > tmax) return null;
        if (tzmin > tmin) tmin = tzmin;
        if (tzmax < tmax) tmax = tzmax;

        return tmin >= 0 ? tmin : (tmax >= 0 ? tmax : null);
    }

    public static Double intersectRaySphere(Vec3d rayOrigin, Vec3d rayDir, Vec3d center, double radius) {
        Vec3d oc = rayOrigin.subtract(center);
        double a = rayDir.dot(rayDir);
        double b = 2.0 * oc.dot(rayDir);
        double c = oc.dot(oc) - radius * radius;
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) return null;
        double t = (-b - Math.sqrt(discriminant)) / (2.0 * a);
        return t >= 0 ? t : null;
    }
}