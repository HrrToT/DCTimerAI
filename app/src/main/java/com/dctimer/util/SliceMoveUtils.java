package com.dctimer.util;

import cs.min2phase.CubieCube;
import cs.min2phase.Util;

public final class SliceMoveUtils {
    private static final int[] SLICE_AXIS = {1, 0, 2};
    private static final int[] SLICE_SIGN = {-1, 1, -1};

    private SliceMoveUtils() {
    }

    public static CubieCube applyMove(CubieCube cube, int move) {
        if (move < 0) {
            return cube;
        }
        if (move < 18) {
            return cube.move(move);
        }
        String facelets = Util.toFaceCube(cube);
        String rotated = applyMoveToFacelets(facelets, move);
        CubieCube next = new CubieCube();
        Util.toCubieCube(rotated, next);
        return next;
    }

    public static String applyMoveToFacelets(String facelets, int move) {
        if (facelets == null || facelets.length() != 54 || move < 18 || move >= 27) {
            return facelets;
        }
        int slice = move / 3 - 6;
        int axis = SLICE_AXIS[slice];
        int quarterTurns = move % 3 == 1 ? 2 : 1;
        int sign = SLICE_SIGN[slice];
        if (move % 3 == 2) {
            sign = -sign;
        }

        char[] state = facelets.toCharArray();
        for (int turn = 0; turn < quarterTurns; turn++) {
            char[] next = new char[54];
            for (int index = 0; index < 54; index++) {
                Sticker sticker = Sticker.fromIndex(index);
                if (belongsToSlice(sticker, axis)) {
                    sticker = sticker.rotate(axis, sign);
                }
                next[sticker.toIndex()] = state[index];
            }
            state = next;
        }
        return new String(state);
    }

    private static boolean belongsToSlice(Sticker sticker, int axis) {
        if (!isSliceEdgeSticker(sticker)) {
            return false;
        }
        switch (axis) {
            case 0:
                return sticker.x == 0;
            case 1:
                return sticker.y == 0;
            default:
                return sticker.z == 0;
        }
    }

    private static boolean isSliceEdgeSticker(Sticker sticker) {
        return Math.abs(sticker.x) + Math.abs(sticker.y) + Math.abs(sticker.z) == 2;
    }

    private static final class Sticker {
        final int x;
        final int y;
        final int z;
        final int nx;
        final int ny;
        final int nz;

        Sticker(int x, int y, int z, int nx, int ny, int nz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }

        static Sticker fromIndex(int index) {
            int face = index / 9;
            int offset = index % 9;
            int row = offset / 3;
            int col = offset % 3;
            switch (face) {
                case 0:
                    return new Sticker(col - 1, 1, row - 1, 0, 1, 0);
                case 1:
                    return new Sticker(1, 1 - row, 1 - col, 1, 0, 0);
                case 2:
                    return new Sticker(col - 1, 1 - row, 1, 0, 0, 1);
                case 3:
                    return new Sticker(col - 1, -1, 1 - row, 0, -1, 0);
                case 4:
                    return new Sticker(-1, 1 - row, col - 1, -1, 0, 0);
                default:
                    return new Sticker(1 - col, 1 - row, -1, 0, 0, -1);
            }
        }

        Sticker rotate(int axis, int sign) {
            int[] pos = rotateVector(x, y, z, axis, sign);
            int[] normal = rotateVector(nx, ny, nz, axis, sign);
            return new Sticker(pos[0], pos[1], pos[2], normal[0], normal[1], normal[2]);
        }

        int toIndex() {
            if (ny == 1) {
                return rowColToIndex(0, z + 1, x + 1);
            }
            if (nx == 1) {
                return rowColToIndex(1, 1 - y, 1 - z);
            }
            if (nz == 1) {
                return rowColToIndex(2, 1 - y, x + 1);
            }
            if (ny == -1) {
                return rowColToIndex(3, 1 - z, x + 1);
            }
            if (nx == -1) {
                return rowColToIndex(4, 1 - y, z + 1);
            }
            return rowColToIndex(5, 1 - y, 1 - x);
        }

        private static int rowColToIndex(int face, int row, int col) {
            return face * 9 + row * 3 + col;
        }

        private static int[] rotateVector(int x, int y, int z, int axis, int sign) {
            switch (axis) {
                case 0:
                    return sign > 0 ? new int[] {x, -z, y} : new int[] {x, z, -y};
                case 1:
                    return sign > 0 ? new int[] {z, y, -x} : new int[] {-z, y, x};
                default:
                    return sign > 0 ? new int[] {-y, x, z} : new int[] {y, -x, z};
            }
        }
    }
}
