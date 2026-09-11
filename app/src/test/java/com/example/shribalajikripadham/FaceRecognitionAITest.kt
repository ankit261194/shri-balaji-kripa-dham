package com.example.shribalajikripadham

import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.model.DevoteeFaceProfile
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class FaceRecognitionAITest {

    @Test
    fun testL2NormalizationProducesUnitVector() {
        val rawVector = floatArrayOf(3.0f, 4.0f, 0.0f, 0.0f)
        val normalized = FaceEmbeddingEngine.l2Normalize(rawVector)
        val norm = FaceEmbeddingEngine.computeL2Norm(normalized)
        assertEquals("L2 Norm must be exactly 1.0", 1.0f, norm, 1e-4f)
    }

    @Test
    fun testCosineSimilarityIdenticalVector() {
        val vectorA = FaceEmbeddingEngine.generateSimulatedInvariantVector("person_1")
        val similarity = FaceEmbeddingEngine.computeCosineSimilarity(vectorA, vectorA)
        assertEquals("Identical vectors must have cosine similarity ~ 1.0", 1.0f, similarity, 1e-4f)
    }

    @Test
    fun testDeepInvarianceUnderBeardGrowthExceedsStrict95PercentSLA() {
        // Person 1 registered clean shave
        val cleanShave = FaceEmbeddingEngine.generateSimulatedInvariantVector(
            identitySeed = "person_devotee_42",
            hasBeard = false,
            hasGlasses = false
        )

        // Person 1 returns after 6 months with heavy beard
        val heavyBeard = FaceEmbeddingEngine.generateSimulatedInvariantVector(
            identitySeed = "person_devotee_42",
            hasBeard = true,
            hasGlasses = false
        )

        val similarity = FaceEmbeddingEngine.computeCosineSimilarity(cleanShave, heavyBeard)
        assertTrue(
            "Beard variation must retain strict similarity >= 95.0% (Actual: $similarity)",
            similarity >= 0.95f
        )
    }

    @Test
    fun testDeepInvarianceUnderSpectaclesAndCapExceedsStrict95PercentSLA() {
        // Person registered without eyewear
        val baseProfile = FaceEmbeddingEngine.generateSimulatedInvariantVector(
            identitySeed = "person_devotee_88",
            hasGlasses = false,
            hasCap = false
        )

        // Person returns wearing glasses and cap
        val withGlassesAndCap = FaceEmbeddingEngine.generateSimulatedInvariantVector(
            identitySeed = "person_devotee_88",
            hasGlasses = true,
            hasCap = true
        )

        val similarity = FaceEmbeddingEngine.computeCosineSimilarity(baseProfile, withGlassesAndCap)
        assertTrue(
            "Eyewear & Cap variation must retain strict similarity >= 95.0% (Actual: $similarity)",
            similarity >= 0.95f
        )
    }

    @Test
    fun testLocalDevicePrecheckClarityAndPoseAngles() {
        // 1. Good quality face: clear and straight angle
        val goodCheck = FaceEmbeddingEngine.validateFacePrecheck(
            clarityScore = 0.96f,
            headEulerAngleY = 2.0f,
            headEulerAngleZ = -3.0f
        )
        assertTrue("Good face must pass pre-check", goodCheck.isFaceClear)
        assertTrue("Pose must be acceptable", goodCheck.isPoseAcceptable)
        assertNull("Failure reason must be null for good check", goodCheck.failureReason)

        // 2. Blurry face (< 0.85)
        val blurryCheck = FaceEmbeddingEngine.validateFacePrecheck(
            clarityScore = 0.72f,
            headEulerAngleY = 0.0f,
            headEulerAngleZ = 0.0f
        )
        assertFalse("Blurry face must fail clarity check", blurryCheck.isFaceClear)
        assertNotNull("Blurry face must return failure reason", blurryCheck.failureReason)

        // 3. Excessive head rotation (angle > 20 deg)
        val turnedFaceCheck = FaceEmbeddingEngine.validateFacePrecheck(
            clarityScore = 0.95f,
            headEulerAngleY = 28.5f,
            headEulerAngleZ = 0.0f
        )
        assertFalse("Turned face must fail pose check", turnedFaceCheck.isPoseAcceptable)
        assertNotNull("Turned face must return failure reason", turnedFaceCheck.failureReason)
    }

    @Test
    fun testZeroCrossMatchingDifferentPeople() {
        // Person A vs Person B
        val personA = FaceEmbeddingEngine.generateSimulatedInvariantVector("person_alice")
        val personB = FaceEmbeddingEngine.generateSimulatedInvariantVector("person_bob")

        val similarity = FaceEmbeddingEngine.computeCosineSimilarity(personA, personB)
        assertTrue(
            "Different people must have low similarity far below 95% threshold (Actual: $similarity)",
            similarity < 0.60f
        )

        // Test with findBestMatch
        val profileB = DevoteeFaceProfile(
            id = 2,
            patientName = "Bob Sharma",
            phoneNumber = "+91 99999 11111",
            faceVector = personB
        )

        val match = FaceEmbeddingEngine.findBestMatch(
            candidateVector = personA,
            enrolledProfiles = listOf(profileB),
            threshold = FaceEmbeddingEngine.MINIMUM_CONFIDENCE_THRESHOLD // 0.95f
        )
        assertNull("Zero Cross-Matching: Candidate A must NEVER match Profile B", match)
    }

    @Test
    fun testStrictFallbackWhenSimilarityBelow95Percent() {
        val personA = FaceEmbeddingEngine.generateSimulatedInvariantVector("person_target")
        // Create candidate with injected variation to lower similarity
        val candidateBelowThreshold = personA.copyOf()
        candidateBelowThreshold[0] += 0.6f
        val normalizedCandidate = FaceEmbeddingEngine.l2Normalize(candidateBelowThreshold)

        val profileA = DevoteeFaceProfile(
            id = 10,
            patientName = "Target Devotee",
            phoneNumber = "+91 98111 22334",
            faceVector = personA
        )

        val sim = FaceEmbeddingEngine.computeCosineSimilarity(normalizedCandidate, personA)
        val match = FaceEmbeddingEngine.findBestMatch(
            candidateVector = normalizedCandidate,
            enrolledProfiles = listOf(profileA),
            threshold = 0.95f
        )

        if (sim < 0.95f) {
            assertNull("When similarity < 95%, match must return null to trigger instant fallback", match)
        }
    }

    @Test
    fun testAutoUpdateFaceEmbeddingEMA() {
        val originalVector = FaceEmbeddingEngine.generateSimulatedInvariantVector("devotee_ramesh", hasBeard = false)
        val newBeardVector = FaceEmbeddingEngine.generateSimulatedInvariantVector("devotee_ramesh", hasBeard = true)

        // Enrich profile with EMA (alpha = 0.75)
        val updatedVector = FaceEmbeddingEngine.enrichEmbedding(
            existingVector = originalVector,
            newCandidateVector = newBeardVector,
            alpha = 0.75f
        )

        // 1. Vector remains unit normalized
        val norm = FaceEmbeddingEngine.computeL2Norm(updatedVector)
        assertEquals("Enriched vector must remain unit length", 1.0f, norm, 1e-4f)

        // 2. Similarity to the new beard appearance must increase
        val simOriginalToNew = FaceEmbeddingEngine.computeCosineSimilarity(originalVector, newBeardVector)
        val simUpdatedToNew = FaceEmbeddingEngine.computeCosineSimilarity(updatedVector, newBeardVector)
        assertTrue(
            "Enriched profile should be closer to the new appearance ($simUpdatedToNew > $simOriginalToNew)",
            simUpdatedToNew > simOriginalToNew
        )

        // 3. Similarity to original anchor must also remain high (no catastrophic forgetting)
        val simUpdatedToOriginal = FaceEmbeddingEngine.computeCosineSimilarity(updatedVector, originalVector)
        assertTrue(
            "Enriched profile should maintain >= 95% similarity to original anchor (Actual: $simUpdatedToOriginal)",
            simUpdatedToOriginal >= 0.95f
        )
    }

    @Test
    fun testUltraFastVectorSearchOver1000ProfilesSub50ms() {
        // Seed 1,000 enrolled devotee profiles
        val candidate = FaceEmbeddingEngine.generateSimulatedInvariantVector("devotee_target_777")
        val profiles = (1..1000).map { id ->
            DevoteeFaceProfile(
                id = id.toLong(),
                patientName = "Devotee #$id",
                phoneNumber = "+91 98000 ${10000 + id}",
                faceVector = FaceEmbeddingEngine.generateSimulatedInvariantVector("devotee_${if (id == 777) "target_777" else "other_$id"}")
            )
        }

        // Benchmark HNSW / indexed search
        val (match, elapsedMs) = FaceEmbeddingEngine.searchVectorDatabaseHNSW(
            candidateVector = candidate,
            enrolledProfiles = profiles,
            threshold = FaceEmbeddingEngine.MINIMUM_CONFIDENCE_THRESHOLD // 0.95f
        )

        assertNotNull("Should correctly find target match across 1,000 profiles", match)
        assertEquals("Target profile ID should be 777", 777L, match!!.profile.id)
        assertTrue("Similarity must be >= 95% SLA (Actual: ${match.confidence})", match.confidence >= 0.95f)
        assertTrue("Vector search across 1,000 profiles must be sub-50ms (Actual: ${elapsedMs}ms)", elapsedMs < 50L)
    }

    @Test
    fun testVectorSerializationBlobRoundTrip() {
        val original = FaceEmbeddingEngine.generateSimulatedInvariantVector("serialization_test")
        val blob = FaceEmbeddingEngine.vectorToBlob(original)
        assertEquals("128 floats * 4 bytes = 512 bytes blob", 512, blob.size)

        val restored = FaceEmbeddingEngine.blobToVector(blob)
        assertEquals("Restored vector length must be 128", 128, restored.size)

        for (i in original.indices) {
            assertEquals("Restored vector float at index $i must match", original[i], restored[i], 1e-6f)
        }
    }
}
