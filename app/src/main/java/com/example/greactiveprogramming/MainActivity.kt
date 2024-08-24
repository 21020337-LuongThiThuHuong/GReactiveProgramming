package com.example.greactiveprogramming

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.greactiveprogramming.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.InputStreamReader

data class Employee(
    val name: String,
    val birthYear: Int,
    val address: String
)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var employeeAdapter: EmployeeAdapter
    private val employees = mutableListOf<Employee>()
    private val employeeList = mutableListOf<Employee>()
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load employees from JSON
        loadEmployeesFromJson()

        // Setup RecyclerView
        employeeAdapter = EmployeeAdapter(employeeList)
        binding.employeeList.layoutManager = LinearLayoutManager(this)
        binding.employeeList.adapter = employeeAdapter

        // Setup Filters
        setupFilters()

        // Setup Search functionality
        setupSearch()

        // Initialize employee management actions
        initializeEmployeeManagement()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                addEmployee()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadEmployeesFromJson() {
        val inputStream = assets.open("employees.json")
        val reader = InputStreamReader(inputStream)
        val employeeListType = object : TypeToken<List<Employee>>() {}.type
        employees.addAll(Gson().fromJson(reader, employeeListType))
        employeeList.addAll(employees)
        reader.close()
    }

    private fun setupFilters() {
        val birthYears = listOf("All", "<1985", "1985-1989", "1990-1994", "1995-2000", ">2000")
        val addresses = mutableListOf("All").apply { addAll(employees.map { it.address }.distinct().sorted()) }

        val birthYearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, birthYears)
        birthYearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.birthYearFilter.adapter = birthYearAdapter

        val addressAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, addresses)
        addressAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.addressFilter.adapter = addressAdapter

        binding.birthYearFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters(binding.searchBox.text.toString())  // Pass current search query
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.addressFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters(binding.searchBox.text.toString())  // Pass current search query
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSearch() {
        binding.searchBox.textChanges()
            .debounce(1000)
            .onEach { query ->
                Log.d("Search", "Query: $query")
                applyFilters(query.toString())  // Use applyFilters with the search query
            }
            .launchIn(lifecycleScope)
    }

    private fun applyFilters(searchQuery: String = "") {
        val selectedBirthYear = binding.birthYearFilter.selectedItem?.toString()
        val selectedAddress = binding.addressFilter.selectedItem?.toString()

        val filteredEmployees = employees.filter { employee ->
            val birthYearAsInt = employee.birthYear.toString().toIntOrNull() ?: -1

            val matchesBirthYear = when (selectedBirthYear) {
                "All" -> true
                "<1985" -> birthYearAsInt < 1985
                "1985-1989" -> birthYearAsInt in 1985..1989
                "1990-1994" -> birthYearAsInt in 1990..1994
                "1995-2000" -> birthYearAsInt in 1995..2000
                ">2000" -> birthYearAsInt > 2000
                else -> false
            }

            val matchesAddress = selectedAddress == "All" || employee.address.equals(selectedAddress, ignoreCase = true)
            val matchesSearchQuery = searchQuery.isBlank() || employee.name.contains(searchQuery, ignoreCase = true)

            matchesBirthYear && matchesAddress && matchesSearchQuery
        }

        employeeAdapter.updateList(filteredEmployees)
    }

    private fun EditText.textChanges(): Flow<CharSequence> {
        return callbackFlow {
            val textWatcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    trySend(s ?: "")
                }

                override fun afterTextChanged(s: android.text.Editable?) {}
            }
            addTextChangedListener(textWatcher)
            awaitClose { removeTextChangedListener(textWatcher) }
        }.conflate()
    }

    // Initialize and handle adding and deleting employees
    private fun initializeEmployeeManagement() {
        employeeAdapter.onDeleteClicked = { employee ->
            deleteEmployee(employee)
        }
    }

    private fun addEmployee() {
        // Logic to add a new employee to the list
        val newEmployee = Employee("New Employee", 1990, "New City")
        employees.add(newEmployee)
        applyFilters() // Reapply filters to update the displayed list
    }

    private fun deleteEmployee(employee: Employee) {
        // Logic to delete an employee from the list
        employees.remove(employee)
        applyFilters() // Reapply filters to update the displayed list
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel() // Cancel any ongoing coroutines
    }
}
