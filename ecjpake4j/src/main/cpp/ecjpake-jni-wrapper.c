#include <jni.h>
#include <malloc.h>
#include "mbedtls/ctr_drbg.h"
#include "mbedtls/entropy.h"
#include "mbedtls/ecjpake.h"
#include "mbedtls/ecp.h"
#include "mbedtls/md.h"
#include "mbedtls/ssl.h"
#include "mbedtls/platform_time.h"


static const mbedtls_ecp_group_id DEFAULT_CURVE_TYPE = MBEDTLS_ECP_DP_SECP256R1;
static const mbedtls_md_type_t DEFAULT_HASH_TYPE = MBEDTLS_MD_SHA256;
static const size_t MAX_BUFFER_SIZE = MBEDTLS_SSL_MAX_CONTENT_LEN;

typedef int (*Fn)(void*, unsigned char*, size_t);


typedef struct {
    mbedtls_entropy_context entropy_context;
    mbedtls_ctr_drbg_context rng_context;
    mbedtls_ecjpake_context ecjpake_context;
    Fn rng_function;
} CryptoComponents;


// The values here are taken from the io.particle.ecjpake4j.Role enum.
// Strings are a more natural choice here, except string handling
// in C is awful and error-prone, and ints are easy.
mbedtls_ecjpake_role role_type_from_int(int32_t role_int) {
    if (role_int == 1) {
        return MBEDTLS_ECJPAKE_SERVER;
    } else {
        return MBEDTLS_ECJPAKE_CLIENT;
    }
}

CryptoComponents* get_components(JNIEnv *env, jobject componentsPtrBuffer) {
    return (*env)->GetDirectBufferAddress(env, componentsPtrBuffer);
}



JNIEXPORT jobject JNICALL
Java_io_particle_ecjpake4j_ECJPakeImpl_createNativeComponents(JNIEnv *env, jobject instance,
                                                              jstring seedData
) {
    CryptoComponents* components = malloc(sizeof(CryptoComponents));

    mbedtls_entropy_init(&(*components).entropy_context);
    mbedtls_ctr_drbg_init(&(*components).rng_context);
    mbedtls_ecjpake_init(&(*components).ecjpake_context);

    components->rng_function = &mbedtls_ctr_drbg_random;

    const char *seed_data = (*env)->GetStringUTFChars(env, seedData, 0);
    jsize seedSize = (*env)->GetStringLength(env, seedData);
    int result = mbedtls_ctr_drbg_seed(
            &components->rng_context,
            mbedtls_entropy_func,
            &components->entropy_context,
            (const unsigned char *) seed_data, (size_t) seedSize
    );

    (*env)->ReleaseStringUTFChars(env, seedData, seed_data);

    if (result != 0) {
        return NULL;
    }

    jobject pointerBuffer = (*env)->NewDirectByteBuffer(
            env,
            (void *) components,
            sizeof(CryptoComponents)
    );
    return pointerBuffer;
}


JNIEXPORT jint JNICALL
Java_io_particle_ecjpake4j_ECJPakeImpl_setupJpake(JNIEnv *env, jobject instance,
                                                  jobject componentsPtrBuffer,
                                                  jint role_,
                                                  jstring secret_
) {
    CryptoComponents* cc = get_components(env, componentsPtrBuffer);

    const char *secret = (*env)->GetStringUTFChars(env, secret_, 0);
    long secret_size = (*env)->GetStringLength(env, secret_);

    int result = mbedtls_ecjpake_setup(
            &cc->ecjpake_context,
            role_type_from_int(role_),
            DEFAULT_HASH_TYPE,
            DEFAULT_CURVE_TYPE,
            (const unsigned char *) secret,
            (size_t) secret_size
    );

    (*env)->ReleaseStringUTFChars(env, secret_, secret);

    return result;
}


JNIEXPORT jbyteArray JNICALL
Java_io_particle_ecjpake4j_ECJPakeImpl_writeRoundOne(JNIEnv *env, jobject instance,
                                                     jobject componentsPointer
) {
    CryptoComponents* cc = get_components(env, componentsPointer);

    size_t bytes_written_to_buffer = 0;
    unsigned char round1_buffer[MAX_BUFFER_SIZE];

    int result = mbedtls_ecjpake_write_round_one(
            &cc->ecjpake_context,
            round1_buffer,
            (size_t) MAX_BUFFER_SIZE,
            &bytes_written_to_buffer,
            cc->rng_function,
            &cc->rng_context
    );

    if (result != 0) {
        return NULL;
    }

    jbyteArray round_one = (*env)->NewByteArray(env, (jsize) bytes_written_to_buffer);
    (*env)->SetByteArrayRegion(
            env,
            round_one,
            0,
            (jsize) bytes_written_to_buffer,
            (const jbyte *) round1_buffer
    );
    return round_one;
}

JNIEXPORT jint JNICALL
Java_io_particle_ecjpake4j_ECJPakeImpl_readRoundOne(JNIEnv *env, jobject instance,
                                                    jobject componentsPointer,
                                                    jbyteArray remoteRoundOneMessageData_) {
    CryptoComponents* cc = get_components(env, componentsPointer);

    jbyte *remote_round1 = (*env)->GetByteArrayElements(env, remoteRoundOneMessageData_, NULL);
    int len = (*env)->GetArrayLength(env, remoteRoundOneMessageData_);

    int result = mbedtls_ecjpake_read_round_one(
            &cc->ecjpake_context,
            (const unsigned char *) remote_round1,
            (size_t) len
    );

    (*env)->ReleaseByteArrayElements(env, remoteRoundOneMessageData_, remote_round1, 0);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_io_particle_ecjpake4j_ECJPakeImpl_writeRoundTwo(JNIEnv *env, jobject instance,
                                                     jobject componentsPointer) {
    CryptoComponents* cc = get_components(env, componentsPointer);

    size_t bytes_written_to_buffer = 0;
    unsigned char round2_buffer[MAX_BUFFER_SIZE];

    int result = mbedtls_ecjpake_write_round_two(
            &cc->ecjpake_context,
            round2_buffer,
            (size_t) MAX_BUFFER_SIZE,
            &bytes_written_to_buffer,
            cc->rng_function,
            &cc->rng_context
    );

    if (result != 0) {
        return NULL;
    }

    jbyteArray round_two = (*env)->NewByteArray(env, (jsize) bytes_written_to_buffer);
    (*env)->SetByteArrayRegion(
            env,
            round_two,
            0,
            (jsize) bytes_written_to_buffer,
            (const jbyte *) round2_buffer
    );
    return round_two;
}

JNIEXPORT jint JNICALL
Java_io_particle_ecjpake4j_ECJPakeImpl_readRoundTwo(JNIEnv *env, jobject instance,
                                                    jobject componentsPointer,
                                                    jbyteArray remoteRoundTwoMessageData_) {
    CryptoComponents* cc = get_components(env, componentsPointer);

    jbyte *remote_round2 = (*env)->GetByteArrayElements(env, remoteRoundTwoMessageData_, NULL);
    int len = (*env)->GetArrayLength(env, remoteRoundTwoMessageData_);

    int result = mbedtls_ecjpake_read_round_two(
            &cc->ecjpake_context,
            (const unsigned char *) remote_round2,
            (size_t) len
    );

    (*env)->ReleaseByteArrayElements(env, remoteRoundTwoMessageData_, remote_round2, 0);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_io_particle_ecjpake4j_ECJPakeImpl_deriveSecret(JNIEnv *env, jobject instance,
                                                    jobject componentsPointer) {
    CryptoComponents* cc = get_components(env, componentsPointer);

    size_t secret_size = mbedtls_md_get_size(cc->ecjpake_context.md_info);

    size_t bytes_written_to_buffer = 0;
    unsigned char secret_buffer[secret_size];

    int result = mbedtls_ecjpake_derive_secret(
            &cc->ecjpake_context,
            secret_buffer, secret_size, &bytes_written_to_buffer,
            cc->rng_function,
            &cc->rng_context
    );

    if (result != 0) {
        return NULL;
    }

    jbyteArray secret = (*env)->NewByteArray(env, (jsize) bytes_written_to_buffer);
    (*env)->SetByteArrayRegion(
            env,
            secret,
            0,
            (jsize) bytes_written_to_buffer,
            (const jbyte *) secret_buffer
    );
    return secret;
}

JNIEXPORT void JNICALL
Java_io_particle_ecjpake4j_ECJPakeImpl_freePointers(JNIEnv *env, jobject instance,
                                                    jobject componentsPointer) {
    CryptoComponents* cc = get_components(env, componentsPointer);

    mbedtls_entropy_free(&cc->entropy_context);
    mbedtls_ctr_drbg_free(&cc->rng_context);
    mbedtls_ecjpake_free(&cc->ecjpake_context);

    free(cc);
}
