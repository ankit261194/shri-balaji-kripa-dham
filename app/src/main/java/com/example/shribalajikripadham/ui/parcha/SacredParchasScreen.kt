package com.example.shribalajikripadham.ui.parcha

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.model.AdminRole
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.shribalajikripadham.data.model.ParchaCategory
import com.example.shribalajikripadham.data.model.SacredParcha
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.SacredParchaPdfGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SacredParchasScreen(
    isHindi: Boolean,
    currentAdmin: Admin? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()

    var activeAdmin by remember { mutableStateOf(currentAdmin) }
    val isSuperAdmin = activeAdmin?.role == AdminRole.SUPER_ADMIN
    val hasParchaAccess = isSuperAdmin || (activeAdmin?.canManageParchas == true)

    var parchasList by remember { mutableStateOf<List<SacredParcha>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<ParchaCategory?>(null) }
    var activeReaderParcha by remember { mutableStateOf<SacredParcha?>(null) }
    var editingParcha by remember { mutableStateOf<SacredParcha?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteConfirmParcha by remember { mutableStateOf<SacredParcha?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showSevadarDelegationDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var sevadarList by remember { mutableStateOf<List<Admin>>(emptyList()) }
    var adminPinInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }

    fun refreshParchas(forceCloudSync: Boolean = false) {
        scope.launch {
            try {
                if (forceCloudSync) {
                    repository.syncLiveParchasFromGitHub()
                }
                val list = if (hasParchaAccess) {
                    repository.getAllAdminParchas()
                } else {
                    repository.getAllPublicParchas()
                }
                parchasList = if (list.isNotEmpty()) list else com.example.shribalajikripadham.ai.SacredParchaEngine.getCanonicalParchas()
            } catch (e: Exception) {
                e.printStackTrace()
                parchasList = com.example.shribalajikripadham.ai.SacredParchaEngine.getCanonicalParchas()
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            val localList = if (hasParchaAccess) repository.getAllAdminParchas() else repository.getAllPublicParchas()
            if (localList.isNotEmpty()) {
                parchasList = localList
            }
            // Fetch live parchas from GitHub in background
            val (synced, cloudList) = repository.syncLiveParchasFromGitHub()
            if (synced || cloudList.isNotEmpty()) {
                val updatedList = if (hasParchaAccess) repository.getAllAdminParchas() else repository.getAllPublicParchas()
                parchasList = if (updatedList.isNotEmpty()) updatedList else cloudList
            }
        }
    }

    // Filtered Parchas
    val displayedParchas = parchasList.filter { p ->
        val matchesCategory = (selectedCategoryFilter == null || p.category == selectedCategoryFilter)
        val query = searchQuery.trim().lowercase()
        val matchesSearch = if (query.isBlank()) true else {
            p.title.lowercase().contains(query) ||
            p.subtitle.lowercase().contains(query) ||
            p.samagriList.any { it.lowercase().contains(query) } ||
            p.category.displayNameHindi.lowercase().contains(query)
        }
        matchesCategory && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "📜 आश्रम पर्चे व दस्तावेज" else "📜 Sacred Documents & Slips",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isHindi) "हवन, मैया का उतारा, अर्जी व नियम पर्चा (A4 PDF)" else "Hawan, Maiya Utara, Arji & Rules (A4 PDF)",
                            fontSize = 11.sp,
                            color = GoldSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                actions = {
                    var isSyncingCloud by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSyncingCloud = true
                                val (ok, list) = repository.syncLiveParchasFromGitHub()
                                isSyncingCloud = false
                                val updated = if (hasParchaAccess) repository.getAllAdminParchas() else repository.getAllPublicParchas()
                                parchasList = if (updated.isNotEmpty()) updated else list
                                Toast.makeText(context, if (ok) "✓ पर्चे क्लाउड से सिंक हो गए!" else "ऑफलाइन पर्चे लोड हैं", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(if (isSyncingCloud) "⏳" else "🔄", fontSize = 18.sp)
                    }
                    if (isSuperAdmin) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SaffronPrimary,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "👑 सुपर एडमिन",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    sevadarList = repository.getAllAdmins().filter { it.role != AdminRole.SUPER_ADMIN }
                                    showSevadarDelegationDialog = true
                                }
                            }
                        ) {
                            Text("👥", fontSize = 18.sp)
                        }
                    } else if (hasParchaAccess) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SaffronPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "🛡️ अधिकृत व्यवस्थापक",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (activeAdmin == null) {
                        TextButton(
                            onClick = { showAdminLoginDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = GoldSecondary)
                        ) {
                            Text("🔐 एडमिन लॉगिन", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonPrimary)
            )
        },
        floatingActionButton = {
            if (hasParchaAccess) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = SaffronPrimary,
                    contentColor = Color.White,
                    icon = { Text("➕", fontSize = 16.sp) },
                    text = { Text(if (isHindi) "नया पर्चा जोड़ें / स्कैन करें" else "Add / Scan Parcha", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFFFDF9))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (isHindi) "🔍 पर्चा या सामग्री खोजें (उदा. हवन, उतारा)..." else "🔍 Search parcha or item...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaroonPrimary,
                    unfocusedBorderColor = GoldSecondary.copy(alpha = 0.5f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = (selectedCategoryFilter == null),
                    onClick = { selectedCategoryFilter = null },
                    label = { Text(if (isHindi) "सभी पर्चे (${parchasList.size})" else "All (${parchasList.size})", fontSize = 11.sp) }
                )
                ParchaCategory.entries.forEach { cat ->
                    val count = parchasList.count { it.category == cat }
                    FilterChip(
                        selected = (selectedCategoryFilter == cat),
                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat },
                        label = { Text("${cat.icon} ${cat.displayNameHindi} ($count)", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // List of Parchas
            if (displayedParchas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📜", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHindi) "कोई पर्चा नहीं मिला।" else "No parchas found.",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        if (hasParchaAccess) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                            ) {
                                Text(if (isHindi) "+ पहला पर्चा जोड़ें / स्कैन करें" else "+ Add First Parcha")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedParchas, key = { it.parchaId }) { parcha ->
                        ParchaCard(
                            isHindi = isHindi,
                            parcha = parcha,
                            isAdmin = hasParchaAccess,
                            onViewDetails = { activeReaderParcha = parcha },
                            onDownloadPdf = {
                                isGeneratingPdf = true
                                scope.launch {
                                    val pdfFile = SacredParchaPdfGenerator.generateA4ParchaPdf(context, parcha)
                                    isGeneratingPdf = false
                                    if (pdfFile != null && pdfFile.exists()) {
                                        repository.incrementParchaDownload(parcha.parchaId)
                                        refreshParchas()
                                        Toast.makeText(context, if (isHindi) "✓ A4 PDF डाउनलोड हो गया: ${pdfFile.name}" else "PDF Downloaded!", Toast.LENGTH_LONG).show()
                                        SacredParchaPdfGenerator.viewOrSharePdf(context, pdfFile, parcha.title)
                                    } else {
                                        Toast.makeText(context, if (isHindi) "PDF जनरेट करने में त्रुटि!" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSharePdf = {
                                scope.launch {
                                    val pdfFile = SacredParchaPdfGenerator.generateA4ParchaPdf(context, parcha)
                                    if (pdfFile != null && pdfFile.exists()) {
                                        SacredParchaPdfGenerator.viewOrSharePdf(context, pdfFile, parcha.title)
                                    }
                                }
                            },
                            onToggleHidden = { isHidden ->
                                scope.launch {
                                    val (ok, resMsg) = repository.toggleParchaHiddenAndPublish(parcha.parchaId, isHidden)
                                    val updated = if (hasParchaAccess) repository.getAllAdminParchas() else repository.getAllPublicParchas()
                                    parchasList = updated
                                    val msg = if (isHidden)
                                        (if (isHindi) "✓ पर्चा भक्तों से छिपा दिया गया व क्लाउड पर अपडेट हो गया!" else "Parcha hidden & synced to cloud!")
                                    else
                                        (if (isHindi) "✓ पर्चा सभी भक्तों के लिए लाइव कर दिया गया है!" else "Parcha is now live to all!")
                                    Toast.makeText(context, if (ok) msg else "$msg ($resMsg)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onEdit = { editingParcha = parcha },
                            onDelete = { deleteConfirmParcha = parcha }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        EditParchaDialog(
            isHindi = isHindi,
            initialParcha = null,
            onDismiss = { showCreateDialog = false },
            onSaveParcha = { newParcha ->
                scope.launch {
                    val (ok, resMsg) = repository.upsertParchaAndPublish(newParcha)
                    val updated = if (hasParchaAccess) repository.getAllAdminParchas() else repository.getAllPublicParchas()
                    parchasList = updated
                    showCreateDialog = false
                    Toast.makeText(context, if (ok) (if (isHindi) "🎉 पर्चा सफलतापूर्वक ऐप व क्लाउड पर लाइव हो गया!" else "Parcha published live!") else "पर्चा सेव हुआ ($resMsg)", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (editingParcha != null) {
        EditParchaDialog(
            isHindi = isHindi,
            initialParcha = editingParcha,
            onDismiss = { editingParcha = null },
            onSaveParcha = { updatedParcha ->
                scope.launch {
                    val (ok, resMsg) = repository.upsertParchaAndPublish(updatedParcha)
                    val updated = if (hasParchaAccess) repository.getAllAdminParchas() else repository.getAllPublicParchas()
                    parchasList = updated
                    editingParcha = null
                    Toast.makeText(context, if (ok) (if (isHindi) "✓ पर्चा अपडेट कर दिया गया व सभी भक्तों के फोन पर लाइव हो गया!" else "Parcha updated & published!") else "पर्चा अपडेट हुआ ($resMsg)", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Reader Dialog
    if (activeReaderParcha != null) {
        ParchaReaderDialog(
            isHindi = isHindi,
            parcha = activeReaderParcha!!,
            onDismiss = { activeReaderParcha = null },
            onDownloadPdf = {
                val p = activeReaderParcha!!
                scope.launch {
                    val pdfFile = SacredParchaPdfGenerator.generateA4ParchaPdf(context, p)
                    if (pdfFile != null && pdfFile.exists()) {
                        repository.incrementParchaDownload(p.parchaId)
                        refreshParchas()
                        Toast.makeText(context, if (isHindi) "✓ A4 PDF डाउनलोड हो गया!" else "PDF Downloaded!", Toast.LENGTH_SHORT).show()
                        SacredParchaPdfGenerator.viewOrSharePdf(context, pdfFile, p.title)
                    }
                }
            }
        )
    }

    // Delete Confirmation
    if (deleteConfirmParcha != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmParcha = null },
            title = { Text(if (isHindi) "पर्चा हटाएं?" else "Delete Parcha?") },
            text = { Text(if (isHindi) "क्या आप '${deleteConfirmParcha!!.title}' को हटाना चाहते हैं? यह सभी भक्तों के फोन से भी हट जाएगा।" else "Are you sure you want to delete this parcha? It will be removed from all users.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val (ok, resMsg) = repository.deleteParchaAndPublish(deleteConfirmParcha!!.parchaId)
                            val updated = if (hasParchaAccess) repository.getAllAdminParchas() else repository.getAllPublicParchas()
                            parchasList = updated
                            deleteConfirmParcha = null
                            Toast.makeText(context, if (ok) (if (isHindi) "✓ पर्चा हटा दिया गया व सभी फोन से हट गया।" else "Parcha deleted & synced.") else "हटाया गया ($resMsg)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteConfirmParcha = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Super Admin: Sevadar Delegation Dialog
    if (showSevadarDelegationDialog) {
        AlertDialog(
            onDismissRequest = { showSevadarDelegationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📜", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "सेवादर पर्चा अधिकार (Super Admin)" else "Sevadar Parcha Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    Text(
                        text = if (isHindi) 
                            "जिस सेवादर को आप पर्चा प्रबंधन (नया जोड़ना, बदलना, हटाना) का अधिकार देना चाहते हैं, उसका स्विच ऑन करें:" 
                        else 
                            "Toggle access for sevadars allowed to manage sacred parchas:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (sevadarList.isEmpty()) {
                        Text(
                            text = if (isHindi) "कोई अन्य सेवादर पंजीकृत नहीं है। एडमिन डैशबोर्ड से सेवादर जोड़ें।" else "No other sevadars found.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sevadarList, key = { it.id }) { sevadar ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (sevadar.canManageParchas) Color(0xFFF1F8E9) else Color(0xFFFAFAFA)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (sevadar.canManageParchas) Color(0xFF81C784) else Color(0xFFE0E0E0))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sevadar.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("📞 ${sevadar.phoneNumber}", fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                text = if (sevadar.canManageParchas) "✓ पर्चा प्रबंधन अधिकृत" else "✕ कोई अधिकार नहीं",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (sevadar.canManageParchas) Color(0xFF2E7D32) else Color(0xFF757575)
                                            )
                                        }
                                        Switch(
                                            checked = sevadar.canManageParchas,
                                            onCheckedChange = { isChecked ->
                                                scope.launch {
                                                    repository.updateAdminParchaPermission(sevadar.id, isChecked)
                                                    sevadarList = repository.getAllAdmins().filter { it.role != AdminRole.SUPER_ADMIN }
                                                    val msg = if (isChecked)
                                                        "✓ ${sevadar.name} को पर्चा प्रबंधन अधिकार दिया गया"
                                                    else
                                                        "✓ ${sevadar.name} से पर्चा अधिकार हटाया गया"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSevadarDelegationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "पूर्ण (Done)" else "Done")
                }
            }
        )
    }

    // Quick Admin Login Dialog for direct parcha management access
    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminLoginDialog = false
                adminPinInput = ""
                loginError = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔐", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "एडमिन सत्यापन" else "Admin Authentication",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isHindi) "पर्चा प्रबंधन हेतु अपना सुरक्षा पिन दर्ज करें:" else "Enter your security PIN to manage parchas:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = { if (it.length <= 6) adminPinInput = it },
                        label = { Text(if (isHindi) "सुरक्षा पिन" else "Security PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (loginError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(loginError!!, color = Color.Red, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminPinInput.isBlank()) return@Button
                        scope.launch {
                            val authenticated = repository.authenticateAdmin(adminPinInput)
                            if (authenticated != null) {
                                if (authenticated.role == AdminRole.SUPER_ADMIN || authenticated.canManageParchas) {
                                    activeAdmin = authenticated
                                    showAdminLoginDialog = false
                                    adminPinInput = ""
                                    loginError = null
                                    refreshParchas()
                                    Toast.makeText(context, "✓ स्वागत है, ${authenticated.name}! पर्चा संपादन सक्रिय किया गया।", Toast.LENGTH_SHORT).show()
                                } else {
                                    loginError = if (isHindi) "⚠️ आपके पास पर्चा प्रबंधन अधिकार नहीं है। कृपया सुपर एडमिन से संपर्क करें।" else "No parcha permission. Contact Super Admin."
                                }
                            } else {
                                loginError = if (isHindi) "❌ गलत पिन दर्ज किया गया।" else "Incorrect PIN."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "सत्यापित करें" else "Verify")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAdminLoginDialog = false
                    adminPinInput = ""
                    loginError = null
                }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun ParchaCard(
    isHindi: Boolean,
    parcha: SacredParcha,
    isAdmin: Boolean,
    onViewDetails: () -> Unit,
    onDownloadPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onToggleHidden: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (parcha.isHidden) Color(0xFFFFF3E0) else Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, if (parcha.isHidden) Color(0xFFFFB74D) else GoldSecondary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Category & Hidden Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (parcha.category) {
                        ParchaCategory.HAWAN -> Color(0xFFE65100)
                        ParchaCategory.UTARA -> Color(0xFF880E4F)
                        ParchaCategory.ARJI_ARDAS -> Color(0xFFBF360C)
                        ParchaCategory.NIYAM_PARHEZ -> Color(0xFF1B5E20)
                        ParchaCategory.AARTI_STUTI -> Color(0xFF311B92)
                        else -> MaroonAccent
                    }
                ) {
                    Text(
                        text = "${parcha.category.icon} ${parcha.category.displayNameHindi}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (parcha.isHidden) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD32F2F)
                    ) {
                        Text(
                            text = "🔒 छिपा हुआ (Hidden)",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = parcha.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaroonPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle
            if (parcha.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parcha.subtitle,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Samagri preview chips
            if (parcha.samagriList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    parcha.samagriList.take(4).forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF5F5F5),
                            border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
                        ) {
                            Text(
                                text = "• $item",
                                fontSize = 10.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (parcha.samagriList.size > 4) {
                        Text("+${parcha.samagriList.size - 4} और", fontSize = 10.sp, color = SaffronPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Admin Toggle Switch
            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (parcha.isHidden) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (parcha.isHidden) "🔒 छिपा हुआ (केवल एडमिन को दिखेगा)" else "🌐 लाइव (सभी भक्तों को दृश्यमान)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (parcha.isHidden) Color(0xFFC62828) else Color(0xFF1B5E20)
                        )
                        Switch(
                            checked = !parcha.isHidden,
                            onCheckedChange = { isLive -> onToggleHidden(!isLive) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("👁️ पढ़ें", fontSize = 12.sp)
                }

                Button(
                    onClick = onDownloadPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    modifier = Modifier.weight(1.4f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("📥 A4 PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                IconButton(
                    onClick = onSharePdf,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("📤", fontSize = 16.sp)
                }

                if (isAdmin) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("✏️", fontSize = 16.sp)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("🗑️", fontSize = 16.sp, color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun ParchaReaderDialog(
    isHindi: Boolean,
    parcha: SacredParcha,
    onDismiss: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(parcha.category.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = parcha.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "श्री बालाजी कृपा धाम, डूँगरा जाट",
                                fontSize = 10.sp,
                                color = GoldSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GoldSecondary.copy(alpha = 0.5f))

                // Scrollable Reader Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Subtitle Box
                    if (parcha.subtitle.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF8E1),
                            border = BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = parcha.subtitle,
                                fontSize = 12.sp,
                                color = MaroonAccent,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Samagri Section
                    if (parcha.samagriList.isNotEmpty()) {
                        Column {
                            Text(
                                text = "🪔 आवश्यक पूजा व अनुष्ठान सामग्री:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaroonPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            parcha.samagriList.forEach { item ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("•", color = SaffronPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item, fontSize = 12.5.sp, color = Color.Black)
                                }
                            }
                        }
                    }

                    // Vidhi Section
                    if (parcha.vidhiSteps.isNotEmpty()) {
                        Column {
                            Text(
                                text = "📜 चरणबद्ध संपूर्ण विधि व नियम:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaroonPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            parcha.vidhiSteps.forEach { step ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(step, fontSize = 12.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }

                    // Precautions
                    if (parcha.precautions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⚠️ महत्वपूर्ण सावधानियाँ व परहेज:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                parcha.precautions.forEach { p ->
                                    Text("• $p", fontSize = 11.5.sp, color = Color.Black)
                                }
                            }
                        }
                    }

                    // Mantras
                    if (parcha.mantraText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF9C4),
                            border = BorderStroke(1.dp, Color(0xFFFBC02D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "सिद्ध मंत्र व जयघोष",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaroonPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = parcha.mantraText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaroonAccent
                                )
                            }
                        }
                    }
                }

                // Footer Buttons
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "बंद करें" else "Close")
                    }

                    Button(
                        onClick = {
                            onDownloadPdf()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(if (isHindi) "📥 A4 PDF डाउनलोड करें" else "📥 Download A4 PDF", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
