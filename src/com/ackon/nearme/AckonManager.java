package com.ackon.nearme;

import android.content.Context;

import com.olivestory.ackon.AckonDataManager;
import com.olivestory.ackon.AckonListener;
import com.olivestory.ackon.AckonService;
import com.olivestory.ackon.AckonUpdateListener;

public class AckonManager {

	private final Context context;

	/**
	 * 생성자
	 * 
	 * @param context
	 */
	public AckonManager(final Context context, final AckonListener ackonListener) {
		this.context = context;

		// 리스너 등록
		AckonDataManager.setAckonListener(ackonListener);

		// 데이터 갱신 주기 120초로 지정
		AckonDataManager.setUpdateTime(120 * 1000);

		// CMS 데이터 갱신
		AckonDataManager.update(context, new AckonUpdateListener() {

			@Override
			public void onUpdateResult(boolean result) {
				// 데이터 갱신 완료 후 서비스 시작
				if (result)
					start();
			}
		});
	}

	/**
	 * 서비스 시작
	 */
	public void start() {
		AckonService.startService(context);
	}

	/**
	 * 서비스 종료
	 */
	public void stop() {
		AckonService.stopService(context);
	}

}
