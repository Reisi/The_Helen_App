/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.thomasR.helen.profile.dfu.data

import android.os.Parcelable
import androidx.annotation.IntRange
import kotlinx.parcelize.Parcelize

sealed class DfuProgress

data object Idle: DfuProgress()
data object Starting: DfuProgress()
data class InProgress(@IntRange(0, 100) val progress: Int): DfuProgress()
data object Completed: DfuProgress()

// TODO error, abort, ...

/*sealed class DfuState {
    data object Idle : DfuState()
    data class InProgress(val status: DfuProgress) : DfuState()
}

sealed class DfuProgress

internal data object InvalidFile : DfuProgress()
internal data object Starting : DfuProgress()
internal data object InitializingDFU : DfuProgress()

@Parcelize
internal data class Uploading(
    val progress: Int = 0,
    val avgSpeed: Float = 0f,
    val currentPart: Int = 0,
    val partsTotal: Int = 0
) : DfuProgress(), Parcelable

internal data object Completed : DfuProgress()
internal data object Aborted : DfuProgress()
internal data class Error(val key: String, val message: String?) : DfuProgress()
*/