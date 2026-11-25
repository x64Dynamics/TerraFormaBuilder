package com.x64Dynamics.tfb.geometry;

import java.util.*;

public final class SplinePath {
    public enum SplineType {
        CATMULL_ROM,
        CUBIC_BEZIER,
        LINEAR
    }

    public record PathSample(
        Vec3d position,
        Vec3d tangent,
        Vec3d normal,
        double t
    ) { }

    private final SplineType type;
    private final List<Vec3d> controlPoints;
    private final boolean closedLoop;

    public SplinePath(SplineType type, List<Vec3d> controlPoints, boolean closedLoop) {
        this.type = Objects.requireNonNull(type, "type");
        this.controlPoints = Collections.unmodifiableList(new ArrayList<>(controlPoints));
        this.closedLoop = closedLoop;
    }

    public static SplinePath fromIntegerPoints(SplineType type, List<Vec3i> points, boolean closedLoop) {
        List<Vec3d> doublePoints = new ArrayList<>();
        for (Vec3i p : points) {
            doublePoints.add(p.toDouble());
        }
        return new SplinePath(type, doublePoints, closedLoop);
    }

    public SplineType type() { return type; }
    public List<Vec3d> controlPoints() { return controlPoints; }
    public boolean isClosedLoop() { return closedLoop; }

    /**
     * Samples points with uniform step count along the spline.
     * Computes exact tangent and perpendicular horizontal normal vectors for cross-sections.
     */
    public List<PathSample> sample(int samplesPerSegment) {
        List<PathSample> samples = new ArrayList<>();
        if (controlPoints.size() < 2) return samples;

        int numSegments = type == SplineType.CUBIC_BEZIER
            ? Math.max(1, (controlPoints.size() - 1) / 3)
            : (closedLoop ? controlPoints.size() : controlPoints.size() - 1);

        for (int seg = 0; seg < numSegments; seg++) {
            int maxS = (seg == numSegments - 1 && !closedLoop) ? samplesPerSegment : samplesPerSegment - 1;

            for (int s = 0; s <= maxS; s++) {
                double localT = (double) s / (double) samplesPerSegment;
                Vec3d pos = evaluatePosition(seg, localT);
                Vec3d tangent = evaluateTangent(seg, localT);

                // Perpendicular 2D normal vector on the horizontal plane
                double tLen = Math.hypot(tangent.x(), tangent.z());
                Vec3d normal = tLen > 1e-6
                    ? new Vec3d(-tangent.z() / tLen, 0.0, tangent.x() / tLen)
                    : new Vec3d(1.0, 0.0, 0.0);

                samples.add(new PathSample(pos, tangent, normal, seg + localT));
            }
        }

        return samples;
    }

    private Vec3d evaluatePosition(int segment, double t) {
        int n = controlPoints.size();
        switch (type) {
            case CUBIC_BEZIER -> {
                // Treats points in groups of 4 (p0, p1, p2, p3)
                int base = segment * 3;
                if (base + 3 >= n) return controlPoints.get(n - 1);
                Vec3d p0 = controlPoints.get(base);
                Vec3d p1 = controlPoints.get(base + 1);
                Vec3d p2 = controlPoints.get(base + 2);
                Vec3d p3 = controlPoints.get(base + 3);
                return interpolateCubicBezier(p0, p1, p2, p3, t);
            }
            case CATMULL_ROM -> {
                Vec3d p0 = getControlPoint(segment - 1);
                Vec3d p1 = getControlPoint(segment);
                Vec3d p2 = getControlPoint(segment + 1);
                Vec3d p3 = getControlPoint(segment + 2);
                return interpolateCatmullRom(p0, p1, p2, p3, t);
            }
            case LINEAR -> {
                Vec3d p1 = getControlPoint(segment);
                Vec3d p2 = getControlPoint(segment + 1);
                return p1.lerp(p2, t);
            }
            default -> throw new IllegalStateException("Unknown spline type: " + type);
        }
    }

    private Vec3d evaluateTangent(int segment, double t) {
        double delta = 0.001;
        Vec3d pBefore = evaluatePosition(segment, Math.max(0.0, t - delta));
        Vec3d pAfter = evaluatePosition(segment, Math.min(1.0, t + delta));
        double dx = pAfter.x() - pBefore.x();
        double dy = pAfter.y() - pBefore.y();
        double dz = pAfter.z() - pBefore.z();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return new Vec3d(0, 0, 1);
        return new Vec3d(dx / len, dy / len, dz / len);
    }

    private Vec3d getControlPoint(int index) {
        int n = controlPoints.size();
        if (closedLoop) {
            int wrapped = ((index % n) + n) % n;
            return controlPoints.get(wrapped);
        }
        if (index < 0) return controlPoints.get(0);
        if (index >= n) return controlPoints.get(n - 1);
        return controlPoints.get(index);
    }

    private static Vec3d interpolateCubicBezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double u = 1.0 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;

        double x = uuu * p0.x() + 3 * uu * t * p1.x() + 3 * u * tt * p2.x() + ttt * p3.x();
        double y = uuu * p0.y() + 3 * uu * t * p1.y() + 3 * u * tt * p2.y() + ttt * p3.y();
        double z = uuu * p0.z() + 3 * uu * t * p1.z() + 3 * u * tt * p2.z() + ttt * p3.z();
        return new Vec3d(x, y, z);
    }

    private static Vec3d interpolateCatmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double x = 0.5 * ((2.0 * p1.x()) + (-p0.x() + p2.x()) * t + (2.0 * p0.x() - 5.0 * p1.x() + 4.0 * p2.x() - p3.x()) * t2 + (-p0.x() + 3.0 * p1.x() - 3.0 * p2.x() + p3.x()) * t3);
        double y = 0.5 * ((2.0 * p1.y()) + (-p0.y() + p2.y()) * t + (2.0 * p0.y() - 5.0 * p1.y() + 4.0 * p2.y() - p3.y()) * t2 + (-p0.y() + 3.0 * p1.y() - 3.0 * p2.y() + p3.y()) * t3);
        double z = 0.5 * ((2.0 * p1.z()) + (-p0.z() + p2.z()) * t + (2.0 * p0.z() - 5.0 * p1.z() + 4.0 * p2.z() - p3.z()) * t2 + (-p0.z() + 3.0 * p1.z() - 3.0 * p2.z() + p3.z()) * t3);
        return new Vec3d(x, y, z);
    }
}
