package com.edlplan.andengine;

import com.edlplan.framework.utils.Factory;
import com.reco1l.legacy.graphics.PathMesh;

import java.util.Stack;

public class SpriteCache {

    public static Cache<PathMesh> trianglePackCache = new Cache<>(100, PathMesh::new);

    public static class Cache<T> {

        private Stack<T> stack;

        private Factory<T> constructor;

        private int maxCacheCount;

        public Cache(int maxCacheCount, Factory<T> constructor) {
            this.constructor = constructor;
            this.maxCacheCount = maxCacheCount;
            stack = new Stack<>();
        }

        public void save(T t) {
            if (stack.size() < maxCacheCount) {
                stack.push(t);
            }
        }

        public T get() {
            if (stack.isEmpty()) {
                return constructor.create();
            } else {
                return stack.pop();
            }
        }

    }

}
