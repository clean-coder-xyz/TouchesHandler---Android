package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.View;

/**
 * Created by lsemenov on 22.09.2014.
 */
public class DeceleratedCollapseAnimationHelper implements BaseExpandCollapseAnimation.Helper {

    public static Builder builder() {
        // use this static method instead of Builder's constructor
        // in order to not write the keyword <new> every time
        return new Builder();
    }

    public static class Builder {
        private Integer accelerationPercentagePerSecond;
        private Integer totalDistance;
        private Integer acceleratedDistance;
        private Float initialVelocity;

        private Builder() {
            // empty constructor
        }

        public Builder accelerationPercentagePerSecond(int accelerationPercentage) {
            this.accelerationPercentagePerSecond = accelerationPercentage;
            return this;
        }

        public Builder totalDistance(int totalDistance) {
            this.totalDistance = totalDistance;
            return this;
        }

        public Builder acceleratedDistance(int acceleratedDistance) {
            this.acceleratedDistance = acceleratedDistance;
            return this;
        }

        public Builder initialVelocity(float initialVelocity) {
            this.initialVelocity = initialVelocity;
            return this;
        }

        public DeceleratedCollapseAnimationHelper build() {
            checkPredicates();
            return new DeceleratedCollapseAnimationHelper(this);
        }

        private void checkPredicates() {
            checkRequiredFieldsAreInitialized();
        }

        private void checkRequiredFieldsAreInitialized() {
            for (Object field : requiredFields()) {
                if (field == null) {
                    throw new IllegalStateException(Builder.class.getName() + ":  Some fields not initialized");
                }
            }
        }

        private Object[] requiredFields() {
            return new Object[] {
                accelerationPercentagePerSecond, totalDistance, acceleratedDistance, initialVelocity
            };
        }
    }


    private final int totalDistance;
    private final float initialVelocity;
    private final float accelerationPerMillisecond;
    private final float acceleratedDuration;
    private final float staticDuration;
    private final float totalDuration;

    private DeceleratedCollapseAnimationHelper(Builder builder) {
        this.totalDistance = builder.totalDistance;
        this.initialVelocity = builder.initialVelocity;
        this.accelerationPerMillisecond = calculateAccelerationPerMillisecond(builder);
        this.acceleratedDuration = calculateAcceleratedDuration(builder, accelerationPerMillisecond);
        this.staticDuration = calculateStaticDuration(builder);
        this.totalDuration = acceleratedDuration + staticDuration;
    }

    private static float calculateAccelerationPerMillisecond(Builder builder) {
        float accelerationPerSecond = builder.initialVelocity * (builder.accelerationPercentagePerSecond / 100.0f);
        return accelerationPerSecond / 1000.0f;
    }

    private static float calculateAcceleratedDuration(Builder builder, float accelerationPerMillisecond) {
        float initialVelocity = builder.initialVelocity;
        float squaredInitialVelocity = initialVelocity * initialVelocity;
        return (float)
                (Math.sqrt(squaredInitialVelocity + 2 * accelerationPerMillisecond * builder.acceleratedDistance) - initialVelocity)
                        / accelerationPerMillisecond;
    }

    private static float calculateStaticDuration(Builder builder) {
        int staticDistance = builder.totalDistance - builder.acceleratedDistance;
        return staticDistance / builder.initialVelocity;
    }

    @Override
    public int getDuration() {
        return (int) totalDuration;
    }

    @Override
    public void setViewOnAnimationStopped(View view) {
        view.setVisibility(View.GONE);
    }

    @Override
    public float getOffsetSize(float interpolatedTime) {
        return totalDistance - getDistanceToMove(interpolatedTime);
    }

    private float getDistanceToMove(float interpolatedTime) {
        float time = totalDuration * interpolatedTime;
        float staticTime = (time <= staticDuration) ? time : staticDuration;
        float distance = staticTime * initialVelocity;
        if (time > staticDuration) {
            float acceleratedTime = time - staticDuration;
            float acceleratedDistance = acceleratedTime *
                    (initialVelocity + accelerationPerMillisecond * acceleratedTime / 2.0f);
            distance += acceleratedDistance;
        }
        return Math.min(totalDistance, (int) distance);
    }


}

