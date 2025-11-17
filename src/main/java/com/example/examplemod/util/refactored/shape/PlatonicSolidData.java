package com.example.examplemod.util.refactored.shape;

import org.joml.Vector3f;

/**
 * 存储柏拉图立体的预计算顶点和边/面数据。
 */
public enum PlatonicSolidData {
    // --- 枚举实例定义 ---
    TETRAHEDRON(
            new Vector3f[]{ new Vector3f(1, 1, 1), new Vector3f(1, -1, -1), new Vector3f(-1, 1, -1), new Vector3f(-1, -1, 1) },
            new int[][]{ {0, 2, 1}, {0, 1, 3}, {0, 3, 2}, {1, 2, 3} },
            new int[][]{ {0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3} }
    ),
    CUBE(
            new Vector3f[]{ new Vector3f(1, 1, 1), new Vector3f(1, -1, 1), new Vector3f(-1, -1, 1), new Vector3f(-1, 1, 1), new Vector3f(1, 1, -1), new Vector3f(1, -1, -1), new Vector3f(-1, -1, -1), new Vector3f(-1, 1, -1) },
            new int[][]{ {0, 1, 2, 3}, {4, 7, 6, 5}, {0, 3, 7, 4}, {1, 5, 6, 2}, {0, 4, 5, 1}, {3, 2, 6, 7} },
            new int[][]{ {0,1},{1,2},{2,3},{3,0}, {4,5},{5,6},{6,7},{7,4}, {0,4},{1,5},{2,6},{3,7} }
    );
    // ... 可以继续添加 OCTAHEDRON, DODECAHEDRON, ICOSAHEDRON ...

    // --- 字段和构造函数 ---
    public final Vector3f[] vertices;
    public final int[][] faces;
    public final int[][] edges;

    PlatonicSolidData(Vector3f[] vertices, int[][] faces, int[][] edges) {
        this.vertices = vertices;
        this.faces = faces;
        this.edges = edges;
    }
}