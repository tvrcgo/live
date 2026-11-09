package com.lizongying.mytv

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorsTest {

    @Test
    fun networkAndTimeoutErrors() {
        assertEquals(
            R.string.play_failed_network,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED),
        )
        assertEquals(
            R.string.play_failed_network,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT),
        )
        assertEquals(
            R.string.play_failed_network,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_TIMEOUT),
        )
    }

    @Test
    fun httpAndMissingSourceErrors() {
        assertEquals(
            R.string.play_failed_http,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS),
        )
        assertEquals(
            R.string.play_failed_http,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND),
        )
    }

    @Test
    fun formatErrors() {
        assertEquals(
            R.string.play_failed_format,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED),
        )
        assertEquals(
            R.string.play_failed_format,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE),
        )
    }

    @Test
    fun decodeErrors() {
        assertEquals(
            R.string.play_failed_decode,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED),
        )
        assertEquals(
            R.string.play_failed_decode,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED),
        )
    }

    @Test
    fun unknownErrorFallsBackToGenericMessage() {
        assertEquals(
            R.string.play_failed,
            PlaybackErrors.messageRes(PlaybackException.ERROR_CODE_UNSPECIFIED),
        )
        assertEquals(R.string.play_failed, PlaybackErrors.messageRes(Int.MIN_VALUE))
    }
}
