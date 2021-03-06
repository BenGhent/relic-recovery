package com.acmerobotics.relicrecovery.vision;

import com.acmerobotics.library.vision.MatOverlay;
import com.acmerobotics.library.vision.VisionCamera;
import com.acmerobotics.relicrecovery.configuration.AllianceColor;

import org.junit.Ignore;
import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CryptoboxTrackerTest {
    @Test
    public void testNonMaximumSuppression() {
        List<Double> values = new ArrayList<>(Arrays.asList(1.0, 1.01, 1.5, 2.0, 3.0, 3.15, 4.0));
        List<Double> actualOutputValues = VisionUtil.nonMaximumSuppression(values, 0.2);
        List<Double> expectedOutputValues = Arrays.asList(1.005, 1.5, 2.0, 3.075, 4.0);

        assertEquals(expectedOutputValues.size(), actualOutputValues.size());
        for (int i = 0; i < expectedOutputValues.size(); i++) {
            assertEquals(expectedOutputValues.get(i), actualOutputValues.get(i), 0.0001);
        }
    }

    @Test
    @Ignore
    public void testSampleImages() {
        OpenCVStaticLoader.loadStaticLibs();

        VisionCamera.Properties properties = new VisionCamera.Properties() {
            @Override
            public double getHorizontalFocalLengthPx(double imageWidth) {
                return 270.451191280832;
            }
        };
        VisionCamera visionCamera = mock(VisionCamera.class);
        when(visionCamera.getProperties()).thenReturn(properties);

        CryptoboxTracker redTracker = new CryptoboxTracker(AllianceColor.RED);
        CryptoboxTracker blueTracker = new CryptoboxTracker(AllianceColor.BLUE);

        redTracker.init(visionCamera);
        blueTracker.init(visionCamera);

        File imageSourceDir = new File("../scripts/new-cryptobox/images");
        File imageOutputDir = new File("../scripts/new-cryptobox/output");
        imageOutputDir.mkdirs();

        for (File imageFile : imageSourceDir.listFiles()) {
            if (imageFile.getName().startsWith(".")) {
                continue;
            }

            CryptoboxTracker tracker;
            if (imageFile.getName().contains("red")) {
                tracker = redTracker;
            } else {
                tracker = blueTracker;
            }

            System.out.println("processing " + imageFile.getName());

            Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());

            tracker.processFrame(image, 0);

            MatOverlay overlay = new MatOverlay(image);
            tracker.drawOverlay(overlay, image.cols(), image.rows(), true);

            Imgcodecs.imwrite(new File(imageOutputDir, imageFile.getName()).getAbsolutePath(), image);
        }
    }
}
