package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParametricFootprint {
    private final WingConfiguration configuration;
    private final List<BoundingBox3i> wingBoxes;
    private final BoundingBox3i totalBounds;

    public ParametricFootprint(WingConfiguration configuration, List<BoundingBox3i> wingBoxes) {
        this.configuration = configuration;
        this.wingBoxes = Collections.unmodifiableList(new ArrayList<>(wingBoxes));

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BoundingBox3i box : wingBoxes) {
            minX = Math.min(minX, box.min().x());
            maxX = Math.max(maxX, box.max().x());
            minY = Math.min(minY, box.min().y());
            maxY = Math.max(maxY, box.max().y());
            minZ = Math.min(minZ, box.min().z());
            maxZ = Math.max(maxZ, box.max().z());
        }

        this.totalBounds = new BoundingBox3i(new Vec3i(minX, minY, minZ), new Vec3i(maxX, maxY, maxZ));
    }

    public WingConfiguration configuration() { return configuration; }
    public List<BoundingBox3i> wingBoxes() { return wingBoxes; }
    public BoundingBox3i totalBounds() { return totalBounds; }

    public static ParametricFootprint create(BoundingBox3i primaryBox, WingConfiguration config) {
        List<BoundingBox3i> wings = new ArrayList<>();
        wings.add(primaryBox);

        int minX = primaryBox.min().x();
        int minY = primaryBox.min().y();
        int minZ = primaryBox.min().z();
        int sx = primaryBox.sizeX();
        int sy = primaryBox.sizeY();
        int sz = primaryBox.sizeZ();

        switch (config) {
            case SINGLE_RECTANGLE -> {
                // Single primary box
            }
            case L_SHAPED -> {
                // Perpendicular wing extending forward from positive X
                int wingW = Math.max(5, sx / 2);
                int wingD = Math.max(6, sz * 2 / 3);
                BoundingBox3i sideWing = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(primaryBox.max().x() - wingW + 1, minY, primaryBox.max().z() + 1),
                    wingW, sy, wingD
                );
                wings.add(sideWing);
            }
            case T_SHAPED -> {
                // Cross wing centered on Z
                int wingW = Math.max(6, sx / 3);
                int wingD = Math.max(6, sz * 2 / 3);
                int cx = (primaryBox.min().x() + primaryBox.max().x()) / 2 - wingW / 2;
                BoundingBox3i frontWing = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(cx, minY, primaryBox.max().z() + 1),
                    wingW, sy, wingD
                );
                wings.add(frontWing);
            }
            case U_SHAPED -> {
                // Left and right extending wings
                int wingW = Math.max(5, sx / 3);
                int wingD = Math.max(6, sz * 2 / 3);
                BoundingBox3i leftWing = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(minX, minY, primaryBox.max().z() + 1),
                    wingW, sy, wingD
                );
                BoundingBox3i rightWing = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(primaryBox.max().x() - wingW + 1, minY, primaryBox.max().z() + 1),
                    wingW, sy, wingD
                );
                wings.add(leftWing);
                wings.add(rightWing);
            }
            case CORNER_TOWER -> {
                // Multi-storey corner tower
                int towerSize = Math.max(4, Math.min(6, Math.min(sx, sz) / 2));
                int towerHeight = sy + 6;
                BoundingBox3i tower = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(primaryBox.max().x() - towerSize + 1, minY, primaryBox.max().z() - towerSize + 1),
                    towerSize, towerHeight, towerSize
                );
                wings.add(tower);
            }
        }

        return new ParametricFootprint(config, wings);
    }
}