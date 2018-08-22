echo "NOTE: If this script fails, try changing the ANDROID_NDK location in the script file to the correct location!"
ANDROID_NDK=$HOME/Library/android-sdk/ndk-bundle
git clone https://github.com/ARMmbed/mbedtls -b mbedtls-2.12.0
mkdir mbedtls_build
mkdir mbedtls_build/arm64-v8a
mkdir mbedtls_build/armeabi-v7a
cd mbedtls

cp configs/config-thread.h include/mbedtls/config.h

cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
        -DANDROID_PLATFORM=android-21 \
        -DANDROID_ABI=arm64-v8a \
        -DENABLE_TESTING=OFF \
        -DCMAKE_INSTALL_PREFIX=../mbedtls_build/arm64-v8a .
make
make install

cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
        -DANDROID_PLATFORM=android-21 \
        -DANDROID_ABI=armeabi-v7a \
        -DENABLE_TESTING=OFF \
        -DCMAKE_INSTALL_PREFIX=../mbedtls_build/armeabi-v7a .
make
make install

cd ..
mv mbedtls_build/armeabi-v7a/lib/*.a distribution/armeabi-v7a
mv mbedtls_build/arm64-v8a/lib/*.a distribution/arm64-v8a

