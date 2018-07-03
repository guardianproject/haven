package org.havenapp.main.sensors.media;


import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Generates a series of video frames, encodes them, decodes them, and tests for
 * significant divergence from the original.
 */
public class VideoEncoder {

    private static final String TAG = "EncodeDecode";
    private static final boolean VERBOSE = false; // lots of logging
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    // Coding
    private static final int FRAME_RATE = 10; // 10fps
    private static final int IFRAME_INTERVAL = 10; // 10 seconds between
    // I-frames
    // size of a frame, in pixels
    private int mWidth = -1;
    private int mHeight = -1;
    // bit rate, in bits per second
    private int mBitRate = -1;
    // largest color component delta seen (i.e. actual vs. expected)
    private int mLargestColorDelta;

    private File outputFile = null;
    private MediaCodec mEncoder;
    private MediaMuxer mMuxer;
    private int mTrackIndex;
    private boolean mMuxerStarted;
    private ArrayList<File> frames;

    public VideoEncoder(ArrayList<File> frames, File outputFile)
    {
        this.frames = frames;
        this.outputFile = outputFile;
    }

    /**
     * Tests streaming of AVC video through the encoder and decoder. Data is
     * encoded from a series of byte[] buffers and decoded into Surfaces. The
     * output is checked for validity.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean encodeDecodeVideoFromBufferToSurface(int width, int height,
                                                        int bitRate) throws Throwable
    {
        setParameters(width, height, bitRate);
        return encodeDecodeVideoFromBuffer();
    }

    /**
     * Sets the desired frame size and bit rate.
     */
    private void setParameters(int width, int height, int bitRate)
    {
        if ((width % 16) != 0 || (height % 16) != 0)
        {
            Log.w(TAG, "WARNING: width or height not multiple of 16");
        }
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
    }

    /**
     * Tests encoding and subsequently decoding video from frames generated into
     * a buffer.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("InlinedApi")
    public boolean encodeDecodeVideoFromBuffer()
            throws Exception
    {
        mLargestColorDelta = -1;
        boolean result = true;
        try
        {
            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
            if (codecInfo == null)
            {
                // Don't fail CTS if they don't have an AVC codec
                Log.e(TAG, "Unable to find an appropriate codec for "
                        + MIME_TYPE);
                return false;
            }
            if (VERBOSE)
                Log.d(TAG, "found codec: " + codecInfo.getName());
            int colorFormat;
            try
            {
                colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
            } catch (Exception e)
            {
                colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
            }
            if (VERBOSE)
                Log.d(TAG, "found colorFormat: " + colorFormat);
            // We avoid the device-specific limitations on width and height by
            // using values that
            // are multiples of 16, which all tested devices seem to be able to
            // handle.
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE,
                    mWidth, mHeight);
            // Set some properties. Failing to specify some of these can cause
            // the MediaCodec
            // configure() call to throw an unhelpful exception.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            if (VERBOSE)
                Log.d(TAG, "format: " + format);
            // Create a MediaCodec for the desired codec, then configure it as
            // an encoder with
            // our desired properties.
            mEncoder = MediaCodec.createByCodecName(codecInfo.getName());
            mEncoder.configure(format, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoder.start();
            // Create a MediaCodec for the decoder, just based on the MIME type.
            // The various
            // format details will be passed through the csd-0 meta-data later
            // on.
            String outputPath = outputFile.getAbsolutePath();
            try
            {
                mMuxer = new MediaMuxer(outputPath,
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException ioe)
            {
                // throw new RuntimeException("MediaMuxer creation failed",
                // ioe);
                ioe.printStackTrace();
            }
            result = doEncodeDecodeVideoFromBuffer(mEncoder, colorFormat);
        } finally
        {
            if (mEncoder != null)
            {
                mEncoder.stop();
                mEncoder.release();
            }
            if (mMuxer != null)
            {
                mMuxer.stop();
                mMuxer.release();
            }
            if (VERBOSE)
                Log.i(TAG, "Largest color delta: " + mLargestColorDelta);
        }
        return result;
    }

    /**
     * Returns the first codec capable of encoding the specified MIME type, or
     * null if no match was found.
     */
    private static MediaCodecInfo selectCodec(String mimeType)
    {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++)
        {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder())
            {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++)
            {
                if (types[j].equalsIgnoreCase(mimeType))
                {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * Returns a color format that is supported by the codec and by this test
     * code. If no match is found, this throws a test failure -- the set of
     * formats known to the test should be expanded for new platforms.
     */
    private static int selectColorFormat(MediaCodecInfo codecInfo,
                                         String mimeType)
    {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++)
        {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat))
            {
                return colorFormat;
            }
        }
        return 0; // not reached
    }

    /**
     * Returns true if this is a color format that this test code understands
     * (i.e. we know how to read and generate frames in this format).
     */
    private static boolean isRecognizedFormat(int colorFormat)
    {
        switch (colorFormat)
        {
            // these are the formats we know how to handle for
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    /**
     * Does the actual work for encoding frames from buffers of byte[].
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("InlinedApi")
    private boolean doEncodeDecodeVideoFromBuffer(MediaCodec encoder,
                                                  int encoderColorFormat)
    {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int generateIndex = 0;
        // yuv format
        byte[] frameData = new byte[mWidth * mHeight * 3 / 2];
        // Loop until the output side is done.
        boolean inputDone = false;
        // If we're not done submitting frames, generate a new one and submit
        // it. By
        // doing this on every loop we're working to ensure that the encoder
        // always has
        // work to do.
        while (!inputDone)
        {
            int inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufIndex >= 0)
            {
                long ptsUsec = computePresentationTime(generateIndex);
                if (generateIndex >= frames.size())
                {
                    // Send an empty frame with the end-of-stream flag set. If
                    // we set EOS
                    // on a frame with data, that frame data will be ignored,
                    // and the
                    // output will be short one frame.
                    encoder.queueInputBuffer(inputBufIndex, 0, 0, ptsUsec,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    inputDone = true;
                    drainEncoder(true, info);
                } else
                {
                    try
                    {
                        generateFrame(generateIndex, encoderColorFormat,
                                frameData);
                    } catch (Exception e)
                    {
                        Log.i(TAG, "meet a different type of image");
                        Arrays.fill(frameData, (byte) 0);
                    }
                    if (VERBOSE)
                        Log.i(TAG, "generateIndex: " + generateIndex
                                + ", size: " + frames.size());
                    ByteBuffer inputBuf = encoderInputBuffers[inputBufIndex];
                    // the buffer should be sized to hold one full frame
                    inputBuf.clear();
                    inputBuf.put(frameData);
                    encoder.queueInputBuffer(inputBufIndex, 0,
                            frameData.length, ptsUsec, 0);
                    drainEncoder(false, info);
                }
                generateIndex++;
            } else
            {
                // either all in use, or we timed out during initial setup
                if (VERBOSE)
                    Log.i(TAG, "input buffer not available");
            }
        }
        return true;
    }

    /**
     * use Muxer to generate mp4 file with data from encoder
     *
     * @param endOfStream
     *            if this is the last frame
     * @param mBufferInfo
     *            the BufferInfo of data from encoder
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void drainEncoder(boolean endOfStream, MediaCodec.BufferInfo mBufferInfo)
    {
        final int TIMEOUT_USEC = 10000;

        if (endOfStream)
        {
            try
            {
                mEncoder.signalEndOfInputStream();
            } catch (Exception e)
            {
            }
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true)
        {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo,
                    TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER)
            {
                // no output available yet
                if (!endOfStream)
                {
                    break; // out of while
                } else
                {
                    if (VERBOSE)
                        Log.i(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
            {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            {
                // should happen before receiving buffers, and should only
                // happen once
                if (mMuxerStarted)
                {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                if (VERBOSE)
                    Log.i(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0)
            {
                if (VERBOSE)
                    Log.i(TAG,
                            "unexpected result from encoder.dequeueOutputBuffer: "
                                    + encoderStatus);
            } else
            {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null)
                {
                    throw new RuntimeException("encoderOutputBuffer "
                            + encoderStatus + " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)
                {
                    // The codec config data was pulled out and fed to the muxer
                    // when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                    if (VERBOSE)
                        Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0)
                {
                    if (!mMuxerStarted)
                    {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    if (VERBOSE)
                        Log.d(TAG, "BufferInfo: " + mBufferInfo.offset + ","
                                + mBufferInfo.size + ","
                                + mBufferInfo.presentationTimeUs);

                    try
                    {
                        mMuxer.writeSampleData(mTrackIndex, encodedData,
                                mBufferInfo);
                    } catch (Exception e)
                    {
                        Log.i(TAG, "Too many frames");
                    }
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                {
                    if (!endOfStream)
                    {
                        if (VERBOSE)
                            Log.i(TAG, "reached end of stream unexpectedly");
                    } else
                    {
                        if (VERBOSE)
                            Log.i(TAG, "end of stream reached");
                    }
                    break; // out of while
                }
            }
        }
    }

    /**
     * Generates data for frame N into the supplied buffer.
     */
    private void generateFrame(int frameIndex, int colorFormat, byte[] frameData)
    {
        // Set to zero. In YUV this is a dull green.
        Arrays.fill(frameData, (byte) 0);

        /**
        Mat mat = Highgui.imread(frames.get(frameIndex).getAbsolutePath());

//		Mat dst = new Mat(mWidth, mHeight * 3 / 2, CvType.CV_8UC1);
        Mat dst = new Mat();
        Imgproc.cvtColor(mat, dst, Imgproc.COLOR_RGBA2YUV_I420);

        // use array instead of mat to improve the speed
        dst.get(0, 0, frameData);

        byte[] temp = frameData.clone();
        int margin = mHeight / 4;
        int location = mHeight;
        int step = 0;
        for (int i = mHeight; i < mHeight + margin; i++)
        {
            for (int j = 0; j < mWidth; j++)
            {
                byte uValue = temp[i * mWidth + j];
                byte vValue = temp[(i + margin) * mWidth + j];

                frameData[location * mWidth + step] = uValue;
                frameData[location * mWidth + step + 1] = vValue;
                step += 2;
                if (step >= mWidth)
                {
                    location++;
                    step = 0;
                }
            }
        }
         **/
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private static long computePresentationTime(int frameIndex)
    {
        long value = frameIndex;
        return 132 + value * 1000000 / FRAME_RATE;
    }
}