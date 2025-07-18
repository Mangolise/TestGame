package net.mangolise.testgame.util;

import net.minestom.server.coordinate.Vec;

public class MathUtils {
    /**
     * Creates a quaternion from a Vec representing Euler angles in degrees.
     * The rotation is applied in the standard game development order:
     * 1. Yaw (Y-axis)
     * 2. Pitch (X-axis)
     * 3. Roll (Z-axis)
     *
     * @param eulerAngles The Vec containing the rotation angles (in degrees) for the x (Pitch), y (Yaw), and z (Roll) axes.
     * @return The final combined quaternion as a float[4] in [x, y, z, w] order.
     */
    public static float[] createQuaternionFromEuler(Vec eulerAngles) {
        // 1. Get angles in radians
        float angleX = (float) Math.toRadians(eulerAngles.x()); // Pitch
        float angleY = (float) Math.toRadians(eulerAngles.y()); // Yaw
        float angleZ = (float) Math.toRadians(eulerAngles.z()); // Roll

        // 2. Create a quaternion for each individual axis rotation
        float[] qx = fromAxisAngle(1f, 0f, 0f, angleX);
        float[] qy = fromAxisAngle(0f, 1f, 0f, angleY);
        float[] qz = fromAxisAngle(0f, 0f, 1f, angleZ);

        // 3. To get an application order of Y -> X -> Z, the multiplication order must be reversed.
        // Final Rotation = (Yaw) * (Pitch) * (Roll)
        float[] qx_qz = multiplyQuaternions(qx, qz);
        return multiplyQuaternions(qy, qx_qz);
    }

    // The fromAxisAngle and multiplyQuaternions helper methods remain the same.
    private static float[] fromAxisAngle(float ax, float ay, float az, float angle) {
        float halfAngle = angle * 0.5f;
        float sinHalfAngle = (float) Math.sin(halfAngle);

        float w = (float) Math.cos(halfAngle);
        float x = ax * sinHalfAngle;
        float y = ay * sinHalfAngle;
        float z = az * sinHalfAngle;

        return new float[]{x, y, z, w};
    }

    private static float[] multiplyQuaternions(float[] q1, float[] q2) {
        float x1 = q1[0], y1 = q1[1], z1 = q1[2], w1 = q1[3];
        float x2 = q2[0], y2 = q2[1], z2 = q2[2], w2 = q2[3];

        float newX = w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2;
        float newY = w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2;
        float newZ = w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2;
        float newW = w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2;

        return new float[]{newX, newY, newZ, newW};
    }
}
