package com.ibm.trl.BBM.mains;

import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.ibm.trl.BBM.mains.Agent.Ability;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;

import ibm.ANACONDA.Core.MatrixUtility;
import ibm.ANACONDA.Core.MyMatrix;

public class ActionEvaluator {
	static Random rand = new Random();
	static NormalDistribution nd = new NormalDistribution();
	static final int numField = GlobalParameter.numField;
	static final boolean verbose = GlobalParameter.verbose;
	double worstScoreThreshold = Math.log(4.9999);

	/**
	 * �A�N�V���������肷��B
	 */
	public int ComputeOptimalAction(int me, int friend, MapInformation map, Ability abs[], double[] worstScores) throws Exception {
		MyMatrix board = map.board;

		AgentEEE[] agentsNow = new AgentEEE[4];
		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				int type = map.getType(x, y);
				if (Constant.isAgent(type)) {
					AgentEEE aaa = new AgentEEE(x, y, type);
					agentsNow[aaa.agentID - 10] = aaa;
				}
			}
		}

		AgentEEE agentMe = agentsNow[me - 10];

		Ability ab = abs[me - 10];

		// �����̈ʒu����A�e�Z���ւ̈ړ��������v�Z���Ă����B
		MyMatrix dis = BBMUtility.ComputeOptimalDistance(board, agentMe.x, agentMe.y, Integer.MAX_VALUE);

		int action_final = -1;
		String reason = "�Ȃ�";

		double[][] safetyScore = new double[4][6];
		safetyScore[me - 10] = worstScores;

		if (verbose) {
			System.out.println("==============================");
			MatrixUtility.OutputMatrix(new MyMatrix(safetyScore));
			System.out.println("==============================");
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// TODO �A�C�e��������Ȃ�A�A�C�e�������ɍs���B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (action_final == -1) {
			int minx = -1, miny = -1, mindis = Integer.MAX_VALUE;
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					int type = map.getType(x, y);
					if (Constant.isItem(type)) {
						int ddd = (int) dis.data[x][y];
						if (ddd < mindis) {
							mindis = ddd;
							minx = x;
							miny = y;
						}
					}
				}
			}

			if (mindis < Integer.MAX_VALUE) {
				int action = BBMUtility.ComputeFirstDirection(dis, minx, miny);
				double score = safetyScore[me - 10][action];
				if (score > worstScoreThreshold) {
					action_final = action;
					reason = "�V�K�B�A�C�e������邽�߂Ɉړ�����B";
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// TODO �؂�����Ȃ�A�؂��󂵂ɍs���B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (action_final == -1) {
			int minx = -1, miny = -1, mindis = Integer.MAX_VALUE;
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					int power = map.getPower(x, y);
					if (power > 0) continue;
					int ddd = (int) dis.data[x][y];
					if (ddd < mindis) {
						int num = BBMUtility.numWoodBrakable(board, x, y, ab.strength);
						if (num > 0) {
							mindis = ddd;
							minx = x;
							miny = y;
						}
					}
				}
			}

			if (action_final == -1 && mindis == 0) {
				// Wood���󂹂�ʒu�ɂ�����A���e��u���B
				if (ab.numBombHold > 0) {
					int action = 5;
					double score = safetyScore[me - 10][action];
					if (score > worstScoreThreshold) {
						action_final = action;
						reason = "�V�K�B�؂��󂷂��߂ɔ��e��ݒu����B";
					}
				}
			}

			if (action_final == -1 && mindis < Integer.MAX_VALUE && mindis > 0) {
				// Wood���󂹂�ꏊ���݂�������A�������Ɉړ�����B
				int action = BBMUtility.ComputeFirstDirection(dis, minx, miny);
				double score = safetyScore[me - 10][action];
				if (score > worstScoreThreshold) {
					action_final = action;
					reason = "�V�K�B�؂��󂷂��߂Ɉړ�����B";
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// TODO ���S�ȑI�������烉���_���ɑI�ԁB
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// TODO ��̑I�����Ŋ댯�ȏ�ԂɊׂ�Ȃ�A�����Ƃ����S�ȃA�N�V���������B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (action_final == -1) {
			double scoreMax = Double.NEGATIVE_INFINITY;
			for (int action = 0; action < 6; action++) {
				double score = safetyScore[me - 10][action];
				if (score > scoreMax) {
					scoreMax = score;
					action_final = action;
					reason = "�����Ƃ����S�ȑI������I�ԁB";
				}
			}

			if (verbose) {
				if (scoreMax <= 2) {
					System.out.println("��΂��󋵁I�I");
				}
			}
		}

		String line = String.format("%s, action=%d", reason, action_final);
		System.out.println(line);

		return action_final;
	}
}
