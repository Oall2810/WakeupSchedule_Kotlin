package com.suda.yzune.wakeupschedule.settings


import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.widget.ModifyTableNameFragment
import es.dmoral.toasty.Toasty

class TimeSettingsFragment : Fragment() {

    var position = 0
    private lateinit var viewModel: TimeSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        val arguments = arguments
        position = arguments!!.getInt("position")
        viewModel = ViewModelProviders.of(activity!!).get(TimeSettingsViewModel::class.java)
        if (viewModel.timeSelectList.isEmpty()) {
            viewModel.initTimeSelectList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_time_settings, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_time_detail)
        initAdapter(recyclerView)
        viewModel.getTimeData(viewModel.timeTableList[position].id).observe(this, Observer {
            if (it == null) return@Observer
            if (it.isEmpty()) {
                viewModel.initTimeTableData(viewModel.timeTableList[position].id)
            } else {
                viewModel.timeList.clear()
                viewModel.timeList.addAll(it)
                recyclerView.adapter?.notifyDataSetChanged()
            }
        })
        return view
    }

    private fun initAdapter(recyclerView: RecyclerView) {
        val adapter = TimeSettingsAdapter(R.layout.item_time_detail, viewModel.timeList)
        adapter.setOnItemClickListener { _, _, position ->
            val selectTimeDialog = SelectTimeDetailFragment.newInstance(this.position, position, adapter)
            selectTimeDialog.isCancelable = false
            selectTimeDialog.show(fragmentManager, "selectTimeDetail")
        }
        adapter.setHeaderView(initHeaderView(adapter))
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
    }

    private fun initHeaderView(adapter: TimeSettingsAdapter): View {
        val view = LayoutInflater.from(activity).inflate(R.layout.item_time_detail_header, null)
        val llLength = view.findViewById<LinearLayout>(R.id.ll_set_length)
        val switch = view.findViewById<Switch>(R.id.s_time_same)
        if (viewModel.timeTableList[position].sameLen) {
            llLength.visibility = View.VISIBLE
        } else {
            llLength.visibility = View.GONE
        }
        switch.isChecked = viewModel.timeTableList[position].sameLen
        switch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.timeTableList[position].sameLen = isChecked
            if (isChecked) {
                llLength.visibility = View.VISIBLE
            } else {
                llLength.visibility = View.GONE
            }
        }

        val tvTimeLen = view.findViewById<TextView>(R.id.tv_time_length)
        val seekBar = view.findViewById<SeekBar>(R.id.sb_time_length)
        seekBar.progress = viewModel.timeTableList[position].courseLen - 30
        tvTimeLen.text = viewModel.timeTableList[position].courseLen.toString()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTimeLen.text = "${progress + 30}"
                viewModel.refreshEndTime(progress + 30)
                viewModel.timeTableList[position].courseLen = progress + 30
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                adapter.notifyDataSetChanged()
            }

        })

        val tvName = view.findViewById<TextView>(R.id.tv_table_name)
        val llName = view.findViewById<LinearLayout>(R.id.ll_table_name)
        tvName.text = viewModel.timeTableList[position].name
        llName.setOnClickListener {
            if (viewModel.timeTableList[position].id == 1) {
                Toasty.error(activity!!.applicationContext, "默认时间表不能改名呢>_<").show()
                return@setOnClickListener
            }
            ModifyTableNameFragment.newInstance(changeListener = object : ModifyTableNameFragment.TableNameChangeListener {
                override fun onFinish(editText: EditText, dialog: Dialog) {
                    if (!editText.text.toString().isEmpty()) {
                        tvName.text = editText.text.toString()
                        viewModel.timeTableList[position].name = editText.text.toString()
                        dialog.dismiss()
                    } else {
                        Toasty.error(activity!!.applicationContext, "名称不能为空哦>_<").show()
                    }
                }

            },
                    titleStr = "时间表名字",
                    string = viewModel.timeTableList[position].name).show(fragmentManager, "timeTableNameDialog")
        }
        return view
    }
}
