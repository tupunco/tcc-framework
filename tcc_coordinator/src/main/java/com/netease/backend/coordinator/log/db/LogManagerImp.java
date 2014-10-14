package com.netease.backend.coordinator.log.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netease.backend.coordinator.log.LogException;
import com.netease.backend.coordinator.log.LogManager;
import com.netease.backend.coordinator.log.LogType;
import com.netease.backend.coordinator.transaction.Action;
import com.netease.backend.coordinator.transaction.Transaction;
import com.netease.backend.coordinator.util.DbUtil;
import com.netease.backend.tcc.error.HeuristicsException;

public class LogManagerImp implements LogManager {

	private DbUtil dbUtil = null;
	private Logger logger = LoggerFactory.getLogger(LogManagerImp.class);
	
	public LogManagerImp() {
		this.dbUtil = new DbUtil();
	}
	
	
	
	@Override
	public void logBegin(Transaction tx, Action action) throws LogException {	
		// Get log Type
		LogType logType = null;
		switch(action) {
		case CONFIRM:
			logType = LogType.TRX_START_CONFIRM;
			break;
		case CANCEL:
			logType = LogType.TRX_START_CANCEL;
			break;
		case EXPIRE:
			logType = LogType.TRX_START_EXPIRE;
			break;
		default:
			throw new LogException("Action Type Error in logBegin");
		}
		
		this.dbUtil.writeLog(tx, logType);
	}

	@Override
	public void logFinish(Transaction tx, Action action) throws LogException {
		// Get log Type
		LogType logType = null;
		switch(action) {
		case CONFIRM:
			logType = LogType.TRX_END_CONFIRM;
			break;
		case CANCEL:
			logType = LogType.TRX_END_CANCEL;
			break;
		case EXPIRE:
			logType = LogType.TRX_END_EXPIRE;
			break;
		default:
			throw new LogException("Action Type Error in logFinish");
		}
		this.dbUtil.writeLog(tx, logType);
	}

	@Override
	public void logRegister(Transaction tx) throws LogException {
		LogType logType = LogType.TRX_BEGIN;
		
		this.dbUtil.writeLog(tx, logType);
	}

	@Override
	public boolean checkExpire(long uuid) throws LogException {
		boolean res = this.dbUtil.checkExpire(uuid);
		return res;
	}

	@Override
	public void logHeuristics(Transaction tx, Action action,
			HeuristicsException e) throws LogException {
		
		try {
			this.dbUtil.writeHeuristicRec(tx, action, e, false);
		} catch (LogException e1) {
			logger.error("Write system heuristic record error", e1);
			this.dbUtil.writeHeuristicRec(tx, action, e, true);
		}
		
		LogType logType = LogType.TRX_HEURESTIC;
		this.dbUtil.writeLog(tx, logType);
	}

	@Override
	public void setCheckpoint(long checkpoint) throws LogException {
		// TODO Auto-generated method stub
		this.dbUtil.setCheckpoint(checkpoint);
	}

	@Override
	public long getCheckpoint() throws LogException {
		// TODO Auto-generated method stub
		long checkpoint = this.dbUtil.getCheckpoint();
		return checkpoint;
	}

	@Override
	public boolean checkActionInRecover(long uuid) throws LogException {
		// TODO Auto-generated method stub
		boolean res = this.dbUtil.checkActionInRecover(uuid);
		return res;
	}

	@Override
	public boolean checkLocalLogMgrAlive() {
		// TODO Auto-generated method stub
		boolean res = this.dbUtil.checkLocaLogMgrAlive();
		return res;
	}

}

