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
	double worstScoreThreshold = Math.log(4.5);
	double attackThreshold = -5;

	/**
	 * �A�N�V���������肷��B
	 */
	public int ComputeOptimalAction(int me, int friend, MapInformation map, Ability abs[], double[][][][] worstScores) throws Exception {
		worstScoreThreshold = 1.3;

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

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// safetyScore���v�Z����B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		double[][] safetyScore = new double[4][6];
		for (int ai = 0; ai < 4; ai++) {
			if (ai == me - 10) {

				int numzero = 6;

				// �����̃X�R�A�͍ň��P�[�X�Ō��ς���B
				if (numzero == 6) {
					for (int a = 0; a < 6; a++) {
						double min = Double.POSITIVE_INFINITY;
						for (int b = 0; b < 6; b++) {
							double num = worstScores[a][b][ai][3];
							if (num == 0) continue;
							double temp = worstScores[a][b][ai][1];
							if (temp < min) {
								min = temp;
							}
						}
						if (min == Double.POSITIVE_INFINITY) min = Double.NEGATIVE_INFINITY;
						safetyScore[ai][a] = min;
					}
				}

				numzero = 0;
				for (int a = 0; a < 6; a++) {
					if (safetyScore[ai][a] == Double.NEGATIVE_INFINITY) numzero++;
				}

				// �ň��P�[�X���S��0�Ȃ�A���傤���Ȃ��̂ŕ��ςŌ��ς���B
				if (numzero == 6) {
					for (int a = 0; a < 6; a++) {
						double sum = Double.NEGATIVE_INFINITY;
						double count = 0;
						for (int b = 0; b < 6; b++) {
							double num = worstScores[a][b][ai][3];
							if (num == 0) continue;
							sum = BBMUtility.add_log(sum, worstScores[a][b][ai][0]);
							count += num;
						}
						double ave;
						if (count == 0) {
							ave = Double.NEGATIVE_INFINITY;
						} else {
							ave = sum - Math.log(count);
						}
						safetyScore[ai][a] = ave;
					}
				}

				numzero = 0;
				for (int a = 0; a < 6; a++) {
					if (safetyScore[ai][a] == Double.NEGATIVE_INFINITY) numzero++;
				}

				if (numzero == 6) {
					System.out.println("���Ŏ��ʂ�");
				}
			} else {
				// �����ȊO�̃X�R�A�́A���ςŌ��ς���B
				for (int a = 0; a < 6; a++) {
					double sum = Double.NEGATIVE_INFINITY;
					double count = 0;
					for (int b = 0; b < 6; b++) {
						double num = worstScores[a][b][ai][3];
						if (num == 0) continue;
						sum = BBMUtility.add_log(sum, worstScores[a][b][ai][0]);
						count += num;
					}
					double ave;
					if (count == 0) {
						ave = Double.POSITIVE_INFINITY;
					} else {
						ave = sum - Math.log(count);
					}
					safetyScore[ai][a] = ave;
				}
			}
		}

		if (verbose) {
			System.out.println("==============================");
			System.out.println("==============================");
			MatrixUtility.OutputMatrix(new MyMatrix(safetyScore));
			System.out.println("==============================");
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �l�߂����Ԃł���΁A�l�߂�B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (true) {

		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �����̈��S���m�ۂ�����ԂŁA������댯�ɂł���P�[�X��T���B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		int action_final = -1;
		String reason = "�Ȃ�";

		if (true) {
			int action_attack = -1;
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;
				double min = Double.POSITIVE_INFINITY;
				int mina = -1;
				for (int a = 0; a < 6; a++) {
					double sss = safetyScore[ai][a];
					double sssme = safetyScore[me - 10][a];
					if (sssme < attackThreshold) continue;
					if (Double.isNaN(sss)) continue;
					if (sss < min) {
						min = sss;
						mina = a;
					}
				}

				if (min < -22) {
					action_attack = mina;
				}
			}
			if (action_attack != -1) {
				System.out.println("�ǂ��l�߉\�Ha=" + action_attack);
				action_final = action_attack;
				reason = "�U������ׂ��B";
			}
		}

		// �y�A�U��
		if (true) {
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;

				double min = Double.POSITIVE_INFINITY;
				int mina = -1;
				int minb = -1;
				for (int a = 0; a < 6; a++) {
					for (int b = 0; b < 6; b++) {
						double num = worstScores[a][b][ai][3];
						if (num == 0) continue;
						double ave = worstScores[a][b][ai][0] - Math.log(num);

						if (ave < min) {
							min = ave;
							mina = a;
							minb = b;
						}
					}
				}

				if (min < -22) {
					System.out.println("�y�A�Œǂ��l�߉\�Hai=" + ai + ", amin=" + mina + ", bmin=" + minb);
					System.out.println("�y�A�Œǂ��l�߉\�Hai=" + ai + ", amin=" + mina + ", bmin=" + minb);
				}
			}
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
