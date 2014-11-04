package com.ackon.nearme;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.olivestory.ackon.Ackon;
import com.olivestory.ackon.AckonInfo;
import com.olivestory.ackon.AckonListener;

public class MainActivity extends ActionBarActivity implements AckonListener {

	/**
	 * 신호레벨 리소스
	 */
	private static final int[] rssiRes = { R.drawable.ic_signal_wifi_statusbar_null_black_48dp,
			R.drawable.ic_signal_wifi_statusbar_1_bar_black_48dp, R.drawable.ic_signal_wifi_statusbar_2_bar_black_48dp,
			R.drawable.ic_signal_wifi_statusbar_3_bar_black_48dp, R.drawable.ic_signal_wifi_statusbar_4_bar_black_48dp };

	/**
	 * 정렬
	 */
	private final static Comparator<Ackon> COMPARATOR = new Comparator<Ackon>() {
		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(Ackon object1, Ackon object2) {
			return collator.compare(String.valueOf(object1.getRssi() * -1), String.valueOf(object2.getRssi() * -1));
		}
	};

	private static final List<Ackon> ackonList = new ArrayList<Ackon>();
	private static final LinkedHashMap<String, Ackon> map = new LinkedHashMap<String, Ackon>();

	private AckonManager ackonManager;
	private TextView uuid, major, minor;
	private ImageView rssi;

	private InfoAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		uuid = (TextView) findViewById(R.id.uuid);
		major = (TextView) findViewById(R.id.major);
		minor = (TextView) findViewById(R.id.minor);
		rssi = (ImageView) findViewById(R.id.rssi);

		adapter = new InfoAdapter();
		ListView listView = (ListView) findViewById(R.id.listView);
		listView.setAdapter(adapter);

		// 메니저를 통해 실행
		ackonManager = new AckonManager(this, this);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// 앱을 실행 할 때만 서비스를 실행하므로 종료 시점에서 서비스 종료 시켜준다.
		if (ackonManager != null)
			ackonManager.stop();
	}

	/**
	 * Ackon 진입시 호출
	 */
	@Override
	public void onAckonEnter(Ackon arg0) {

	}

	/**
	 * Ackon 이탈 시 호출
	 */
	@Override
	public void onAckonExit(Ackon arg0) {

	}

	@Override
	public void onAckonResult(int arg0, Ackon arg1) {

	}

	/**
	 * 감지되고 있는 Ackon이 있는경우 onAckonUpdate를 호출한다. Ackon이 없을 경우 더이상 호출되지 않는다.<br>
	 * 거리가 가장가까운 Ackon을 표시하기 위해서는 onAckonUpdate 메소드를 구현한다.
	 */
	@Override
	public void onAckonUpdate(final Ackon ackon) {
		if (ackon == null)
			return;

		// 수신되는 Ackon 추가 및 정렬
		synchronized (map) {
			map.put(ackon.getName(), ackon);
			ackonList.clear();
			ackonList.addAll(map.values());
			Collections.sort(ackonList, COMPARATOR);
		}

		// 가장 가까운 Ackon을 선택
		final Ackon near = ackonList.get(0);
		final AckonInfo info = ackonList.get(0).getAckonInfo();
		if (near != null && info != null) {
			// 백그라운드 쓰레드에서 동작하기 때문에 runOnUiThread를 이용해서 UI로 반영해야 한다.
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					getSupportActionBar().setTitle(getString(R.string.app_name) + " : " + info.getAlias());
					getSupportActionBar().setSubtitle("RSSI : " + near.getRssi());

					// CMS 데이터 반영
					adapter.setData(info.getKeys(), info.getValues());

					// 가장 가까운 Ackon이 바뀔경우 애니메이션 전환 효과를 준다.
					Object tag = uuid.getTag();
					if (tag != null) {
						String name = (String) tag;
						if (near.getName().equals(name) == false) {
							findViewById(R.id.layout).startAnimation(
									AnimationUtils.loadAnimation(getApplicationContext(), R.anim.abc_slide_in_bottom));
						}
					}

					uuid.setTag(near.getName());
					uuid.setText(near.getUuid());
					major.setText(String.valueOf(near.getMajor()));
					minor.setText(String.valueOf(near.getMinor()));

					/*
					 * 안드로이드에서는 거리 계산 정확도가 다소 떨어지기 때문에 RSSI값을 기준으로 계산을 한다. BLE 신호 특성상 안정적인 신호를 주지 못하기 때문에 거리 표현은
					 * getProximityLevel() 메소드를 이용해서 가까운 레벨값을 이용해 표시한다. 1의 가까울 수록 가깝다. 각 level의 실제 RSSI 값은 -15 간격이다.
					 */
					switch (near.getProximityLevel()) {

					case 1: // - 15
					case 2:
						// - 30 이내
						rssi.setImageResource(rssiRes[4]);
						break;

					case 3:
						// - 45 이내
						rssi.setImageResource(rssiRes[3]);
						break;

					case 4:
						// - 60 이내
						rssi.setImageResource(rssiRes[2]);
						break;
					case 5:
						// - 75 이내
						rssi.setImageResource(rssiRes[1]);
						break;

					case 6:
					case 7:
					case 8:
					case 9:
						rssi.setImageResource(rssiRes[0]);
						break;

					default:
						rssi.setImageResource(rssiRes[0]);
						break;
					}
				}
			});

		}
	}

	/**
	 * 어뎁터 클래스, 일반적인 리스트 구성 어뎁터
	 * 
	 * @author android
	 * 
	 */
	class InfoAdapter extends BaseAdapter {

		private String[] key, value;

		public void setData(String[] key, String[] value) {
			this.key = key;
			this.value = value;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return value != null ? value.length : 0;
		}

		@Override
		public String getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			Holder holder = null;
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.row_data_item, parent, false);

				holder = new Holder(convertView);

				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}

			// Ackon의 CMS 데이터 반영
			holder.key.setText(key[position]);
			holder.value.setText(value[position]);

			return convertView;
		}
	}

	// 홀더
	class Holder {

		TextView key, value;

		public Holder(View convertView) {
			key = (TextView) convertView.findViewById(R.id.key);
			value = (TextView) convertView.findViewById(R.id.value);
		}

	}
}
