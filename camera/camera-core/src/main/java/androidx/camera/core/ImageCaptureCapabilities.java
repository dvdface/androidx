/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.core;

import androidx.annotation.RestrictTo;

/**
 * ImageCaptureCapabilities is used to query {@link ImageCapture} use case capabilities on the
 * device.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ImageCaptureCapabilities {
    /**
     * Returns if the takePicture() call in {@link ImageCapture} is capable of outputting post
     * view images ahead of final images. If supported, apps can enable the postview using
     * {@link ImageCapture.Builder#setPostviewEnabled(boolean)}.
     */
    boolean isPostviewSupported();

    /**
     * Returns if the takePicture() call in {@link ImageCapture} is capable of notifying the
     * onCaptureProcessProgressed callback to the apps.
     */
    boolean isCaptureProcessProgressSupported();
}
