package com.x64Dynamics.tfb.architectural.interior.layouts;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
public class ParametricFootprint {
    private final WingConfiguration configuration;
    private final List<BoundingBox3i> wingBoxes;
    private final List<BuildingSection> sections;
    private final BoundingBox3i totalBounds;

    public ParametricFootprint(WingConfiguration configuration, List<BoundingBox3i> wingBoxes, List<BuildingSection> sections) {
        this.configuration = configuration;
        this.wingBoxes = Collections.unmodifiableList(new ArrayList<>(wingBoxes));
        this.sections = Collections.unmodifiableList(new ArrayList<>(sections));

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

    public ParametricFootprint(WingConfiguration configuration, List<BoundingBox3i> wingBoxes) {
        this(configuration, wingBoxes, createDefaultSections(wingBoxes));
    }

    private static List<BuildingSection> createDefaultSections(List<BoundingBox3i> boxes) {
        List<BuildingSection> list = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            boolean isPrim = (i == 0);
            list.add(new BuildingSection(
                isPrim ? "primary" : "wing_" + i,
                boxes.get(i),
                BuildingSection.SectionShape.RECTANGLE,
                RoofGenerator.RoofType.GABLE,
                1,
                isPrim
            ));
        }
        return list;
    }

    public WingConfiguration configuration() { return configuration; }
    public List<BoundingBox3i> wingBoxes() { return wingBoxes; }
    public List<BuildingSection> sections() { return sections; }
    public BoundingBox3i totalBounds() { return totalBounds; }

    public static ParametricFootprint create(BoundingBox3i primaryBox, WingConfiguration config) {
        List<BoundingBox3i> wings = new ArrayList<>();
        List<BuildingSection> sections = new ArrayList<>();

        wings.add(primaryBox);
        sections.add(BuildingSection.primaryRectangle(primaryBox, RoofGenerator.RoofType.GABLE));

        int minX = primaryBox.min().x();
        int minY = primaryBox.min().y();
        int minZ = primaryBox.min().z();
        int sx = primaryBox.sizeX();
        int sy = primaryBox.sizeY();
        int sz = primaryBox.sizeZ();

        switch (config) {
            case SINGLE_RECTANGLE -> { }
            case L_SHAPED -> {
                int wingW = Math.max(5, sx / 2);
                int wingD = Math.max(6, sz * 2 / 3);
                BoundingBox3i sideWing = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(primaryBox.max().x() - wingW + 1, minY, primaryBox.max().z() + 1),
                    wingW, sy, wingD
                );
                wings.add(sideWing);
                sections.add(BuildingSection.attachedWing("side_wing", sideWing, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
            }
            case T_SHAPED -> {
                int wingW = Math.max(6, sx / 3);
                int wingD = Math.max(6, sz * 2 / 3);
                int cx = (primaryBox.min().x() + primaryBox.max().x()) / 2 - wingW / 2;
                BoundingBox3i frontWing = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(cx, minY, primaryBox.max().z() + 1),
                    wingW, sy, wingD
                );
                wings.add(frontWing);
                sections.add(BuildingSection.attachedWing("front_wing", frontWing, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
            }
            case U_SHAPED -> {
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
                sections.add(BuildingSection.attachedWing("left_wing", leftWing, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
                sections.add(BuildingSection.attachedWing("right_wing", rightWing, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
            }
            case CORNER_TOWER -> {
                int towerSize = Math.max(4, Math.min(6, Math.min(sx, sz) / 2));
                int towerHeight = sy + 6;
                BoundingBox3i tower = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(primaryBox.max().x() - towerSize + 1, minY, primaryBox.max().z() - towerSize + 1),
                    towerSize, towerHeight, towerSize
                );
                wings.add(tower);
                sections.add(BuildingSection.attachedWing("corner_tower", tower, BuildingSection.SectionShape.CYLINDER, RoofGenerator.RoofType.CONICAL));
            }
            case CROSS_SHAPED -> {
                int armW = Math.max(5, sx / 3);
                int armD = Math.max(5, sz / 3);
                int midX = (primaryBox.min().x() + primaryBox.max().x()) / 2 - armW / 2;
                int midZ = (primaryBox.min().z() + primaryBox.max().z()) / 2 - armD / 2;

                BoundingBox3i northArm = BoundingBox3i.fromOriginAndSize(new Vec3i(midX, minY, primaryBox.min().z() - armD), armW, sy, armD);
                BoundingBox3i southArm = BoundingBox3i.fromOriginAndSize(new Vec3i(midX, minY, primaryBox.max().z() + 1), armW, sy, armD);
                BoundingBox3i eastArm = BoundingBox3i.fromOriginAndSize(new Vec3i(primaryBox.max().x() + 1, minY, midZ), armW, sy, armD);
                BoundingBox3i westArm = BoundingBox3i.fromOriginAndSize(new Vec3i(primaryBox.min().x() - armW, minY, midZ), armW, sy, armD);

                wings.add(northArm);
                wings.add(southArm);
                wings.add(eastArm);
                wings.add(westArm);
                sections.add(BuildingSection.attachedWing("north_arm", northArm, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
                sections.add(BuildingSection.attachedWing("south_arm", southArm, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
                sections.add(BuildingSection.attachedWing("east_arm", eastArm, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
                sections.add(BuildingSection.attachedWing("west_arm", westArm, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
            }
            case H_SHAPED -> {
                int wingW = Math.max(5, sx / 3);
                int wingD = Math.max(8, sz + 4);
                BoundingBox3i leftLongWing = BoundingBox3i.fromOriginAndSize(new Vec3i(minX - wingW, minY, minZ - 2), wingW, sy, wingD);
                BoundingBox3i rightLongWing = BoundingBox3i.fromOriginAndSize(new Vec3i(primaryBox.max().x() + 1, minY, minZ - 2), wingW, sy, wingD);
                wings.add(leftLongWing);
                wings.add(rightLongWing);
                sections.add(BuildingSection.attachedWing("left_parallel", leftLongWing, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
                sections.add(BuildingSection.attachedWing("right_parallel", rightLongWing, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE));
            }
            case OCTAGONAL_EXTENSION -> {
                int rotundaSize = Math.max(6, Math.min(sx, sz));
                int rotundaX = (primaryBox.min().x() + primaryBox.max().x()) / 2 - rotundaSize / 2;
                BoundingBox3i rotunda = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(rotundaX, minY, primaryBox.max().z() + 1),
                    rotundaSize, sy, rotundaSize
                );
                wings.add(rotunda);
                sections.add(BuildingSection.attachedWing("rotunda", rotunda, BuildingSection.SectionShape.OCTAGON, RoofGenerator.RoofType.DOME));
            }
            case COURTYARD_COMPOUND -> {
                int wingThick = Math.max(4, Math.min(sx, sz) / 4);
                BoundingBox3i north = BoundingBox3i.fromOriginAndSize(new Vec3i(minX, minY, minZ), sx, sy, wingThick);
                BoundingBox3i south = BoundingBox3i.fromOriginAndSize(new Vec3i(minX, minY, primaryBox.max().z() - wingThick + 1), sx, sy, wingThick);
                BoundingBox3i west = BoundingBox3i.fromOriginAndSize(new Vec3i(minX, minY, minZ), wingThick, sy, sz);
                BoundingBox3i east = BoundingBox3i.fromOriginAndSize(new Vec3i(primaryBox.max().x() - wingThick + 1, minY, minZ), wingThick, sy, sz);

                wings.clear();
                sections.clear();
                wings.add(north);
                wings.add(south);
                wings.add(west);
                wings.add(east);
                sections.add(BuildingSection.primaryRectangle(north, RoofGenerator.RoofType.HIP));
                sections.add(BuildingSection.attachedWing("south_cloister", south, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.HIP));
                sections.add(BuildingSection.attachedWing("west_cloister", west, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.HIP));
                sections.add(BuildingSection.attachedWing("east_cloister", east, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.HIP));
            }
        }

        return new ParametricFootprint(config, wings, sections);
    }
}
