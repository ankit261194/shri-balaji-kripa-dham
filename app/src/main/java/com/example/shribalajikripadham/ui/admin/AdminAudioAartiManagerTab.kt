package com.example.shribalajikripadham.ui.admin

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.data.sacred.SacredTrack
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import com.example.shribalajikripadham.theme.sacredOutlinedTextFieldColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAudioAartiManagerTab(
    isHindi: Boolean,
    repository: AshramRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tracksList by remember { mutableStateOf<List<SacredTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showTrackEditDialog by remember { mutableStateOf(false) }
    var editingTrack by remember { mutableStateOf<SacredTrack?>(null) }
    var trackToDelete by remember { mutableStateOf<SacredTrack?>(null) }
    var viewLyricsTrack by remember { mutableStateOf<SacredTrack?>(null) }

    // Form fields
    var formTitleHindi by remember { mutableStateOf("") }
    var formTitleEnglish by remember { mutableStateOf("") }
    var formSubtitleHindi by remember { mutableStateOf("") }
    var formDurationText by remember { mutableStateOf("") }
    var formAudioUrl by remember { mutableStateOf("") }
    var formLyricsHindi by remember { mutableStateOf("") }
    var formIsPublished by remember { mutableStateOf(true) }
    var formDisplayOrder by remember { mutableStateOf("1") }
    var formYtQuery by remember { mutableStateOf("") }
    var isUploadingAudio by remember { mutableStateOf(false) }

    // Preview Player
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewingTrackId by remember { mutableLongStateOf(-1L) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (ignored: Exception) {}
        previewPlayer = null
        previewingTrackId = -1L
        isPreviewPlaying = false
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    fun startPreview(track: SacredTrack) {
        if (previewingTrackId == track.id && isPreviewPlaying) {
            stopPreview()
            return
        }
        stopPreview()
        if (track.audioUrl.isBlank()) {
            Toast.makeText(context, if (isHindi) "ऑडियो URL उपलब्ध नहीं है" else "Audio URL not available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(track.audioUrl)
                setOnPreparedListener {
                    it.start()
                    previewingTrackId = track.id
                    isPreviewPlaying = true
                }
                setOnCompletionListener {
                    stopPreview()
                }
                setOnErrorListener { _, _, _ ->
                    stopPreview()
                    Toast.makeText(context, if (isHindi) "ऑडियो प्लेबैक में त्रुटि" else "Audio playback error", Toast.LENGTH_SHORT).show()
                    true
                }
                prepareAsync()
            }
            previewPlayer = player
        } catch (e: Exception) {
            Toast.makeText(context, "Player Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadData() {
        scope.launch {
            isLoading = true
            tracksList = repository.getSacredTracks(publishedOnly = false)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
        // Silent sync from Hostinger
        scope.launch {
            repository.syncSacredTracksFromHostinger(admin = true)
            tracksList = repository.getSacredTracks(publishedOnly = false)
        }
    }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploadingAudio = true
                Toast.makeText(context, if (isHindi) "📤 ऑडियो फाइल सर्वर पर अपलोड हो रही है..." else "Uploading audio file to server...", Toast.LENGTH_SHORT).show()
                try {
                    val tempFile = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val cacheFile = File(context.cacheDir, "upload_audio_${System.currentTimeMillis()}.mp3")
                        FileOutputStream(cacheFile).use { out ->
                            inputStream?.copyTo(out)
                        }
                        cacheFile
                    }

                    val uploadedUrl = repository.uploadSacredAudio(tempFile)
                    if (!uploadedUrl.isNullOrBlank()) {
                        formAudioUrl = uploadedUrl
                        Toast.makeText(context, if (isHindi) "✅ ऑडियो फाइल सफलतापूर्वक अपलोड हो गई!" else "Audio file uploaded successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, if (isHindi) "❌ ऑडियो अपलोड विफल रहा। कृपया इंटरनेट जांचें।" else "Audio upload failed.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isUploadingAudio = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaroonPrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎵", fontSize = 26.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "आरती व भजन प्रबंधन (Audio Studio)" else "Sacred Audio & Aarti Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isHindi) "प्रामाणिक MP3 ऑडियो, शुद्ध लिरिक्स व लाइव प्रकाशन" else "Manage authentic MP3s, lyrics & instant publish",
                                fontSize = 11.5.sp,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingTrack = null
                            formTitleHindi = ""
                            formTitleEnglish = ""
                            formSubtitleHindi = ""
                            formDurationText = "05:00"
                            formAudioUrl = ""
                            formLyricsHindi = ""
                            formIsPublished = true
                            formDisplayOrder = (tracksList.size + 1).toString()
                            formYtQuery = ""
                            showTrackEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "➕ नया ऑडियो जोड़ें" else "➕ Add New Audio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                val (ok, list) = repository.syncSacredTracksFromHostinger(admin = true)
                                tracksList = list
                                isLoading = false
                                if (ok) {
                                    Toast.makeText(context, if (isHindi) "✅ सर्वर से ${list.size} ट्रैक सिंक हुए!" else "Synced ${list.size} tracks!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, if (isHindi) "सर्वर से सिंक करने में असमर्थ" else "Sync failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("🔄 सिंक करें", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaroonPrimary)
            }
        } else if (tracksList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🚩", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "वर्तमान में कोई ऑडियो या आरती अपलोड नहीं है" else "No sacred audio tracks uploaded yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaroonPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "उपरोक्त '➕ नया ऑडियो जोड़ें' बटन दबाकर आश्रम की प्रामाणिक MP3 आरती, भजन व सही पाठ अपलोड करें।"
                        else "Click 'Add New Audio' above to upload authentic MP3 audio and verified lyrics.",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tracksList, key = { it.id }) { track ->
                    val isThisPlaying = previewingTrackId == track.id && isPreviewPlaying

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isThisPlaying) Color(0xFFFFF8E1) else Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isThisPlaying) SaffronPrimary else if (track.isPublished) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Order Badge
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (track.isPublished) MaroonPrimary else Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${track.displayOrder}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.titleHindi.ifBlank { track.titleEnglish },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = MaroonPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (track.subtitleHindi.isNotBlank()) {
                                        Text(
                                            text = track.subtitleHindi,
                                            fontSize = 11.5.sp,
                                            color = Color.DarkGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Status Tag
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (track.isPublished) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ) {
                                    Text(
                                        text = if (track.isPublished) "✅ लाइव" else "⏸ ड्राफ्ट",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (track.isPublished) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Play / Stop Preview Button
                                    Button(
                                        onClick = { startPreview(track) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isThisPlaying) MaroonPrimary else SaffronPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = if (isThisPlaying) "⏸ रोकें" else "▶ सुनें",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    // Lyrics Viewer Button
                                    OutlinedButton(
                                        onClick = { viewLyricsTrack = track },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("📖 लिरिक्स", fontSize = 11.sp, color = MaroonAccent)
                                    }
                                }

                                Row {
                                    // Edit Button
                                    IconButton(
                                        onClick = {
                                            editingTrack = track
                                            formTitleHindi = track.titleHindi
                                            formTitleEnglish = track.titleEnglish
                                            formSubtitleHindi = track.subtitleHindi
                                            formDurationText = track.durationText
                                            formAudioUrl = track.audioUrl
                                            formLyricsHindi = track.lyricsHindi
                                            formIsPublished = track.isPublished
                                            formDisplayOrder = track.displayOrder.toString()
                                            formYtQuery = track.youtubeSearchQuery
                                            showTrackEditDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("✏️", fontSize = 16.sp)
                                    }

                                    Spacer(Modifier.width(4.dp))

                                    // Delete Button
                                    IconButton(
                                        onClick = { trackToDelete = track },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("🗑️", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- TRACK EDIT / ADD DIALOG ---
    if (showTrackEditDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUploadingAudio) showTrackEditDialog = false },
            title = {
                Text(
                    text = if (editingTrack != null)
                        (if (isHindi) "पावन आरती / भजन संपादित करें" else "Edit Sacred Track")
                    else
                        (if (isHindi) "नया पावन ऑडियो / आरती जोड़ें" else "Add New Sacred Track"),
                    fontWeight = FontWeight.Bold,
                    color = MaroonPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = formTitleHindi,
                        onValueChange = { formTitleHindi = it },
                        label = { Text("शीर्षक (Hindi Title) *", color = Color(0xFF333333)) },
                        placeholder = { Text("उदा. आरती श्री बालाजी महाराज की") },
                        singleLine = true,
                        colors = sacredOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formSubtitleHindi,
                        onValueChange = { formSubtitleHindi = it },
                        label = { Text("गायक / उपशीर्षक (Singer / Subtitle)", color = Color(0xFF333333)) },
                        placeholder = { Text("उदा. स्वर: आश्रम मंडल • प्रातःकालीन महाआरती") },
                        singleLine = true,
                        colors = sacredOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = formDurationText,
                            onValueChange = { formDurationText = it },
                            label = { Text("अवधि (MM:SS)", color = Color(0xFF333333)) },
                            placeholder = { Text("05:30") },
                            singleLine = true,
                            colors = sacredOutlinedTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = formDisplayOrder,
                            onValueChange = { formDisplayOrder = it },
                            label = { Text("क्रम (Order #)", color = Color(0xFF333333)) },
                            placeholder = { Text("1") },
                            singleLine = true,
                            colors = sacredOutlinedTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Audio File Upload Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        border = BorderStroke(1.dp, SaffronPrimary)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🎧 MP3 ऑडियो फाइल अपलोड करें",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = MaroonPrimary
                            )
                            Spacer(Modifier.height(6.dp))

                            Button(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isUploadingAudio,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isUploadingAudio) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("अपलोड हो रही है...", fontSize = 12.sp)
                                } else {
                                    Text("📁 फोन से ऑडियो फाइल चुनें (MP3/M4A)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (formAudioUrl.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "🔗 लिंक: $formAudioUrl",
                                    fontSize = 10.sp,
                                    color = Color(0xFF1B5E20),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formAudioUrl,
                        onValueChange = { formAudioUrl = it },
                        label = { Text("अथवा डायरेक्ट ऑडियो URL लिंक", color = Color(0xFF333333)) },
                        placeholder = { Text("https://example.com/audio.mp3") },
                        singleLine = true,
                        colors = sacredOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formLyricsHindi,
                        onValueChange = { formLyricsHindi = it },
                        label = { Text("प्रामाणिक संपूर्ण आरती / भजन पाठ (Verified Lyrics)", color = Color(0xFF333333)) },
                        placeholder = { Text("यहाँ संपूर्ण शुद्ध व प्रामाणिक आरती पाठ दर्ज करें...") },
                        minLines = 6,
                        maxLines = 14,
                        colors = sacredOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Publish Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("भक्तों के लिए लाइव प्रकाशित करें", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("चालू होने पर ऐप में सभी को तुरंत सुनाई देगा", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = formIsPublished,
                            onCheckedChange = { formIsPublished = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaroonPrimary, checkedTrackColor = SaffronPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formTitleHindi.isBlank()) {
                            Toast.makeText(context, "कृपया शीर्षक दर्ज करें!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (formAudioUrl.isBlank()) {
                            Toast.makeText(context, "कृपया ऑडियो फाइल अपलोड करें अथवा URL दर्ज करें!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            val trackKey = editingTrack?.trackKey?.ifBlank { null }
                                ?: ("track_" + System.currentTimeMillis())

                            val track = SacredTrack(
                                id = editingTrack?.id ?: 0L,
                                trackKey = trackKey,
                                titleHindi = formTitleHindi.trim(),
                                titleEnglish = formTitleEnglish.trim().ifBlank { formTitleHindi.trim() },
                                subtitleHindi = formSubtitleHindi.trim(),
                                durationText = formDurationText.trim().ifBlank { "05:00" },
                                audioUrl = formAudioUrl.trim(),
                                lyricsHindi = formLyricsHindi.trim(),
                                isPublished = formIsPublished,
                                displayOrder = formDisplayOrder.toIntOrNull() ?: 1,
                                youtubeSearchQuery = formYtQuery.trim().ifBlank { "${formTitleHindi.trim()} Shri Balaji Kripa Dham" }
                            )

                            val (ok, msg) = repository.saveSacredTrack(track)
                            showTrackEditDialog = false
                            loadData()
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text("💾 सुरक्षित व प्रकाशित करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTrackEditDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // --- VIEW LYRICS DIALOG ---
    if (viewLyricsTrack != null) {
        val t = viewLyricsTrack!!
        AlertDialog(
            onDismissRequest = { viewLyricsTrack = null },
            title = {
                Text(
                    text = t.titleHindi,
                    fontWeight = FontWeight.Bold,
                    color = MaroonPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (t.lyricsHindi.isBlank()) {
                        Text(
                            text = "इस ट्रैक के लिए अभी लिखित पाठ दर्ज नहीं किया गया है। 'संपादित करें' बटन दबाकर पाठ जोड़ें।",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = t.lyricsHindi,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            color = Color.Black
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewLyricsTrack = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text("बंद करें")
                }
            }
        )
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (trackToDelete != null) {
        val t = trackToDelete!!
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text("🗑️ ट्रैक हटाने की पुष्टि", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Text("क्या आप निश्चित रूप से '${t.titleHindi}' को हटाना चाहते हैं? यह डेटाबेस और भक्तों के ऐप से हट जाएगा।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val (ok, msg) = repository.deleteSacredTrack(t.id)
                            trackToDelete = null
                            loadData()
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("हाँ, हटाएं", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { trackToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
