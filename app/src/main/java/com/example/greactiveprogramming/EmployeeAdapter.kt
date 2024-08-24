package com.example.greactiveprogramming

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.greactiveprogramming.databinding.ItemBinding

class EmployeeAdapter(
    private val employees: MutableList<Employee>,
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {
    var onDeleteClicked: ((Employee) -> Unit)? = null

    inner class EmployeeViewHolder(
        private val binding: ItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(employee: Employee) {
            binding.employeeName.text = employee.name
            binding.employeeBirthyear.text = "Năm sinh: ${employee.birthYear}"
            binding.employeeAddress.text = "Quê quán: ${employee.address}"
            binding.deleteButton.setOnClickListener {
                onDeleteClicked?.invoke(employee)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): EmployeeViewHolder {
        val binding = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmployeeViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: EmployeeViewHolder,
        position: Int,
    ) {
        holder.bind(employees[position])
    }

    override fun getItemCount() = employees.size

    fun updateList(newEmployees: List<Employee>) {
        employees.clear()
        employees.addAll(newEmployees)
        notifyDataSetChanged()
    }
}
