package com.example.trilogidirectory

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.trilogidirectory.network.ApiClient
import com.example.trilogidirectory.network.Mahasiswa
import com.example.trilogidirectory.ui.theme.TrilogiDirectoryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.UnknownHostException

// ==========================================
// 1. VIEWMODEL
// ==========================================
class MahasiswaViewModel : ViewModel() {
    var listMahasiswa by mutableStateOf<List<Mahasiswa>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        fetchMahasiswa()
    }

    fun fetchMahasiswa() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.instance.getAllMahasiswa()
                if (response.isSuccessful) {
                    listMahasiswa = (response.body() ?: emptyList()).reversed()
                } else {
                    errorMessage = "Server Error: HTTP ${response.code()}"
                }
            } catch (e: UnknownHostException) {
                errorMessage = "Tidak ada koneksi internet."
            } catch (e: Exception) {
                errorMessage = "Gagal memuat data: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun simpanMahasiswaBaru(nama: String, nim: String, prodi: String, semester: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                val dataBaru = Mahasiswa(
                    nama = nama,
                    nim = nim,
                    prodi = prodi,
                    semester = semester,
                    foto = "https://i.pravatar.cc/150?u=$nim"
                )
                val response = ApiClient.instance.tambahMahasiswa(dataBaru)
                if (response.isSuccessful) {
                    fetchMahasiswa()
                } else {
                    errorMessage = "Gagal menyimpan data (HTTP ${response.code()})"
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = "Error POST: ${e.localizedMessage}"
                isLoading = false
            }
        }
    }

    fun updateMahasiswa(
        id: String,
        nama: String,
        nim: String,
        prodi: String,
        semester: String,
        fotoLama: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                val dataUpdate = Mahasiswa(
                    id = id,
                    nama = nama,
                    nim = nim,
                    prodi = prodi,
                    semester = semester,
                    foto = fotoLama ?: "https://i.pravatar.cc/150?u=$nim"
                )
                val response = ApiClient.instance.updateMahasiswa(id, dataUpdate)
                if (response.isSuccessful) {
                    fetchMahasiswa()
                } else {
                    errorMessage = "Gagal memperbarui data (HTTP ${response.code()})"
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = "Error PUT: ${e.localizedMessage}"
                isLoading = false
            }
        }
    }

    fun hapusMahasiswa(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                val response = ApiClient.instance.hapusMahasiswa(id)
                if (response.isSuccessful) {
                    fetchMahasiswa()
                } else {
                    errorMessage = "Gagal menghapus data (HTTP ${response.code()})"
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = "Error DELETE: ${e.localizedMessage}"
                isLoading = false
            }
        }
    }
}

// ==========================================
// 2. MAIN ACTIVITY
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrilogiDirectoryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DirectoryScreen()
                }
            }
        }
    }
}

// ==========================================
// 3. STATEFUL COMPOSABLE
// ==========================================
@Composable
fun DirectoryScreen(viewModel: MahasiswaViewModel = viewModel()) {
    val context = LocalContext.current

    DirectoryContent(
        listMahasiswa = viewModel.listMahasiswa,
        isLoading = viewModel.isLoading,
        errorMessage = viewModel.errorMessage,
        onRetry = { viewModel.fetchMahasiswa() },
        onTambahData = { nama, nim, prodi, semester ->
            viewModel.simpanMahasiswaBaru(nama, nim, prodi, semester)
            Toast.makeText(context, "Menyimpan data...", Toast.LENGTH_SHORT).show()
        },
        onUpdateData = { id, nama, nim, prodi, semester, foto ->
            viewModel.updateMahasiswa(id, nama, nim, prodi, semester, foto)
            Toast.makeText(context, "Memperbarui data...", Toast.LENGTH_SHORT).show()
        },
        onDeleteData = { mhs ->
            viewModel.hapusMahasiswa(mhs.id)
            Toast.makeText(context, "Menghapus ${mhs.nama}...", Toast.LENGTH_SHORT).show()
        },
        onItemClick = { mhs ->
            Toast.makeText(
                context,
                "${mhs.nama} | ${mhs.prodi ?: "-"} (Smt ${mhs.semester ?: "-"})",
                Toast.LENGTH_SHORT
            ).show()
        }
    )
}

// ==========================================
// 4. STATELESS COMPOSABLE (UI)
// ==========================================
@Composable
fun DirectoryContent(
    listMahasiswa: List<Mahasiswa>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onTambahData: (String, String, String, String) -> Unit,
    onUpdateData: (String, String, String, String, String, String?) -> Unit,
    onDeleteData: (Mahasiswa) -> Unit,
    onItemClick: (Mahasiswa) -> Unit
) {
    var namaInput by rememberSaveable { mutableStateOf("") }
    var nimInput by rememberSaveable { mutableStateOf("") }
    var prodiInput by rememberSaveable { mutableStateOf("") }
    var semesterInput by rememberSaveable { mutableStateOf("") }

    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingFoto by rememberSaveable { mutableStateOf<String?>(null) }

    fun resetForm() {
        namaInput = ""
        nimInput = ""
        prodiInput = ""
        semesterInput = ""
        editingId = null
        editingFoto = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Trilogi Directory (CRUD MockAPI)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Form Input (Tambah Baru / POST maupun Edit / PUT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (editingId != null)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (editingId != null) "Edit Data Mahasiswa (PUT)" else "Tambah Mahasiswa Baru (POST)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = namaInput,
                        onValueChange = { namaInput = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nimInput,
                        onValueChange = { nimInput = it },
                        label = { Text("NIM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prodiInput,
                        onValueChange = { prodiInput = it },
                        label = { Text("Program Studi") },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = semesterInput,
                        onValueChange = { semesterInput = it },
                        label = { Text("Semester") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (namaInput.isNotBlank() && nimInput.isNotBlank() &&
                                prodiInput.isNotBlank() && semesterInput.isNotBlank()
                            ) {
                                if (editingId != null) {
                                    onUpdateData(
                                        editingId!!,
                                        namaInput,
                                        nimInput,
                                        prodiInput,
                                        semesterInput,
                                        editingFoto
                                    )
                                } else {
                                    onTambahData(namaInput, nimInput, prodiInput, semesterInput)
                                }
                                resetForm()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (editingId != null) "Update Data" else "Simpan ke Server")
                    }

                    if (editingId != null) {
                        OutlinedButton(onClick = { resetForm() }) {
                            Text("Batal")
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // 3 WAJIB UI STATES
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) {
                        Text("Coba Lagi")
                    }
                }
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listMahasiswa) { mhs ->
                        MahasiswaCard(
                            mahasiswa = mhs,
                            onClick = { onItemClick(mhs) },
                            onEditClick = {
                                editingId = mhs.id
                                editingFoto = mhs.foto
                                namaInput = mhs.nama
                                nimInput = mhs.nim
                                prodiInput = mhs.prodi ?: ""
                                semesterInput = mhs.semester ?: ""
                            },
                            onDeleteClick = { onDeleteData(mhs) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MahasiswaCard(
    mahasiswa: Mahasiswa,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = mahasiswa.foto,
                contentDescription = "Foto ${mahasiswa.nama}",
                placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                error = painterResource(id = R.drawable.ic_launcher_foreground),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mahasiswa.nama,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "NIM: ${mahasiswa.nim}",
                    fontSize = 12.sp
                )
                Text(
                    text = "${mahasiswa.prodi ?: "-"} Semester ${mahasiswa.semester ?: "-"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Menggunakan TextButton bawaan Material 3 (tanpa material-icons)
            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Edit", fontSize = 12.sp)
                }
                TextButton(
                    onClick = onDeleteClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus", fontSize = 12.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DirectoryPreview() {
    TrilogiDirectoryTheme {
        DirectoryContent(
            listMahasiswa = listOf(
                Mahasiswa(
                    id = "1",
                    nama = "Alita Putri",
                    nim = "22105001",
                    prodi = "Sistem Informasi",
                    semester = "5"
                ),
                Mahasiswa(
                    id = "2",
                    nama = "Budi Santoso",
                    nim = "22105002",
                    prodi = "Teknik Informatika",
                    semester = "3"
                )
            ),
            isLoading = false,
            errorMessage = null,
            onRetry = {},
            onTambahData = { _, _, _, _ -> },
            onUpdateData = { _, _, _, _, _, _ -> },
            onDeleteData = {},
            onItemClick = {}
        )
    }
}