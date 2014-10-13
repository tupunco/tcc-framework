package com.netease.backend.coordinator.recover.db;



import java.util.List;

import com.netease.backend.coordinator.id.IdGenerator;
import com.netease.backend.coordinator.log.LogManager;
import com.netease.backend.coordinator.log.LogRecord;
import com.netease.backend.coordinator.log.LogScanner;
import com.netease.backend.coordinator.log.LogType;
import com.netease.backend.coordinator.log.db.LogScannerImp;

import com.netease.backend.coordinator.recover.RecoverManager;
import com.netease.backend.coordinator.transaction.Transaction;
import com.netease.backend.coordinator.transaction.TxTable;
import com.netease.backend.coordinator.util.LogUtil;
import com.netease.backend.tcc.Procedure;
import com.netease.backend.tcc.error.CoordinatorException;

public class RecoverManagerImp implements RecoverManager {
	private TxTable txTable = null;
	private LogManager logMgr = null;
	private IdGenerator idGenerator = null;

	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}

	public void setTxTable(TxTable txTable) {
		this.txTable = txTable;
	}

	public void setLogMgr(LogManager logMgr) {
		this.logMgr = logMgr;
	}

	@Override
	public void init() throws CoordinatorException {
		// TODO Auto-generated method stub
		long checkpoint = logMgr.getCheckpoint();
		LogScanner logScanner = new LogScannerImp();
		logScanner.beginScan(checkpoint);
		long maxUuid = 0;
		while (logScanner.hasNext()){
			LogRecord logRec = logScanner.next();
			long uuid = logRec.getTrxId();
			LogType logType = logRec.getLogType();
			long timestamp = logRec.getTimestamp();
			byte[] procs = logRec.getProcs();
			Transaction trx = this.txTable.get(uuid);
			if (trx == null) {
				// if trx not exist in txTable, than put a new one
				List<Procedure> expireProcs = null;
				if (logType == LogType.TRX_BEGIN)
					expireProcs = LogUtil.deserialize(procs);
				trx = new Transaction(uuid, expireProcs);
				this.txTable.put(trx);
			}
			
			// set tx attributes
			switch(logType) {
			case TRX_BEGIN:
				trx.setCreateTime(timestamp);
				break;
			case TRX_START_CONFIRM:
				trx.confirm(LogUtil.deserialize(procs));
				trx.setBeginTime(timestamp);
				break;
			case TRX_START_CANCEL:
				trx.cancel(LogUtil.deserialize(procs));
				trx.setBeginTime(timestamp);
				break;
			case TRX_START_EXPIRE:
				trx.expire();
				break;
			case TRX_END_CONFIRM:
			case TRX_END_CANCEL:
			case TRX_END_EXPIRE:
			case TRX_HEURESTIC:
				this.txTable.remove(uuid);
				break;
				
			}
			
			// update max Uuid
			if (this.idGenerator.isUuidOwn(trx.getUUID()) &&
					trx.getUUID() > maxUuid){
				maxUuid = trx.getUUID();
			}
		}
		
		logScanner.endScan();
		
		// set max Uuid
		this.idGenerator.setUUIDofLastDown(maxUuid);
	}

}
