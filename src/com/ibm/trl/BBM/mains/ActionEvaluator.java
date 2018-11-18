/********************************************
 * (C) Copyright IBM Corp. 2018
 ********************************************/
package com.ibm.trl.BBM.mains;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.ibm.trl.BBM.mains.Agent.Ability;
import com.ibm.trl.BBM.mains.Agent.LastAgentPosition;
import com.ibm.trl.BBM.mains.Agent.ModelParameter;
import com.ibm.trl.BBM.mains.BombTracker.Node;
import com.ibm.trl.BBM.mains.ForwardModel.Pack;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;
import com.ibm.trl.BBM.mains.WorstScoreEvaluator.ScoreResult;

import ibm.ANACONDA.Core.MatrixUtility;
import ibm.ANACONDA.Core.MyMatrix;

public class ActionEvaluator {
	static Random rand = new Random();
	static NormalDistribution nd = new NormalDistribution();
	static final int numField = GlobalParameter.numField;
	static final boolean verbose = GlobalParameter.verbose;

	ModelParameter param;

	int[] actionLog = new int[1000];
	int actionLogIndex = 0;
	int mugenLoopRecoveryKikan = 0;

	public ActionEvaluator(ModelParameter param) {
		this.param = param;

		for (int i = 0; i < actionLog.length; i++) {
			actionLog[i] = rand.nextInt();
		}
	}

	/**
	 * �A�N�V���������肷��B
	 */
	public int ComputeOptimalAction(boolean collapse, int frame, int me, int friend, int maxPower, Ability[] abs, MapInformation map, BombTracker.Node[][] bombMap, MyMatrix flameLife,
			MyMatrix lastLook, LastAgentPosition[] laps, ScoreResult sr) throws Exception {

		double usualMoveThreshold = param.usualThreshold;
		double attackThreshold = param.attackThreshold;

		// int ooo = 0;

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// ��{�ϐ��̌v�Z
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
		MyMatrix dis = BBMUtility.ComputeOptimalDistance(board, agentMe.x, agentMe.y, Integer.MAX_VALUE, false);
		MyMatrix dis2 = BBMUtility.ComputeOptimalDistance(board, agentMe.x, agentMe.y, Integer.MAX_VALUE, true);

		int numVisibleTeam = 0;
		int numVisibleEnemy = 0;
		for (int ai = 0; ai < 4; ai++) {
			AgentEEE aaa = agentsNow[ai];
			if (aaa == null) continue;
			if (ai == me - 10) {
				numVisibleTeam++;
			} else if (ai == friend - 10) {
				numVisibleTeam++;
			} else {
				numVisibleEnemy++;
			}
		}

		int numAliveTeam = 0;
		int numAliveEnemy = 0;
		for (int ai = 0; ai < 4; ai++) {
			if (abs[ai].isAlive == false) continue;
			if (ai == me - 10) {
				numAliveTeam++;
			} else if (ai == friend - 10) {
				numAliveTeam++;
			} else {
				numAliveEnemy++;
			}
		}

		int numBrakableWood = 0;
		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				int type = (int) board.data[x][y];
				if (type == Constant.Wood) {
					boolean find = false;
					for (int[] vec : GlobalParameter.onehopList) {
						if (vec[0] == 0) continue;
						int dx = vec[1];
						int dy = vec[2];
						int x2 = x + dx;
						int y2 = y + dy;
						if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) continue;
						double ddd = dis.data[x2][y2];
						if (ddd > 1000) continue;
						find = true;
						break;
					}

					if (find) {
						numBrakableWood++;
					}
				}
			}
		}

		// �A�N�V�������������[�v�ɓ����Ă��邩���m����B
		int actionNG = -1;
		if (true) {
			int numTry = 30;
			for (int delay = 1; delay <= 4; delay++) {
				int num = 0;
				for (int i = 0; i < numTry; i++) {
					int index1 = actionLogIndex - i - 1;
					if (index1 < 0) index1 += actionLog.length;
					int index2 = actionLogIndex - i - 1 - delay;
					if (index2 < 0) index2 += actionLog.length;
					int a1 = actionLog[index1];
					int a2 = actionLog[index2];
					if (a1 == a2) {
						num++;
					}
				}
				if (num == numTry) {
					int index = actionLogIndex - delay;
					if (index < 0) index += actionLog.length;
					actionNG = actionLog[index];
					mugenLoopRecoveryKikan = 10;
					break;
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// safetyScore���v�Z����B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// ���ς̕��ςŌ��ς���B
		double[][] safetyScoreAverage = new double[4][6];
		for (int ai = 0; ai < 4; ai++) {
			for (int a = 0; a < 6; a++) {
				double num = sr.singleScore[a][ai].num;
				double sss = sr.singleScore[a][ai].sum / num;
				if (num == 0) {
					safetyScoreAverage[ai][a] = Double.NaN;
				} else {
					safetyScoreAverage[ai][a] = sss;
				}
			}
		}

		// �ň��P�[�X�̍ň��P�[�X�Ō��ς���B
		double[][] safetyScoreWorst = new double[4][6];
		for (int ai = 0; ai < 4; ai++) {
			for (int a = 0; a < 6; a++) {
				double num = sr.singleScore[a][ai].num;
				double sss = sr.singleScore[a][ai].min;
				if (num == 0) {
					safetyScoreWorst[ai][a] = Double.NaN;
				} else {
					// TODO
					// safetyScoreWorst[ai][a] = sss + safetyScoreAverage[ai][a] * 1.0e-10;
					safetyScoreWorst[ai][a] = sss;
				}
			}
		}

		if (verbose) {
			System.out.println("==============================");
			MatrixUtility.OutputMatrix(new MyMatrix(safetyScoreWorst));
			System.out.println("==============================");
		}

		if (verbose) {
			System.out.println("==============================");
			MatrixUtility.OutputMatrix(new MyMatrix(safetyScoreAverage));
			System.out.println("==============================");
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �A�N�V�������胋�[�`��
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		int actionFinal = -1;
		String reason = "�Ȃ�";

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �Q�F�P�̂Ƃ��ɁA�����ł���ꍇ�́A���s����B(^o^)/
		// �P��
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (actionFinal == -1 && numAliveTeam == 2 && numAliveEnemy == 1) {
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;

				double max = 0;
				double min = Double.POSITIVE_INFINITY;
				int amin = -1;
				for (int a = 0; a < 6; a++) {
					double scoreFriend = safetyScoreWorst[friend - 10][a];
					double scoreEnemy = safetyScoreAverage[ai][a];
					if (Double.isNaN(scoreEnemy)) continue;
					if (Double.isNaN(scoreFriend) == false && scoreFriend < attackThreshold) continue;
					if (scoreEnemy < min) {
						min = scoreEnemy;
						amin = a;
					}
					if (scoreEnemy > max) {
						max = scoreEnemy;
					}
				}

				// �ق��Ƃ��Ă����ʁB
				if (max == 0) continue;

				// �����ł���B
				if (amin != -1 && min == 0) {
					actionFinal = amin;
					reason = "�������܂��B(^o^)/";
					System.out.println(reason);
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// ���_����̋��݌������W�b�N��^���Ă݂�H�H
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (actionFinal == -1) {
			if (false) {
				StatusHolder sh = new StatusHolder();
				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						if (bombMap[x][y] == null) continue;
						Node node = bombMap[x][y];
						for (int dir = 0; dir < 5; dir++) {
							if (node.dirs[dir]) {
								sh.setBomb(x, y, -1, node.life, dir, node.power);
								break;
							}
						}
					}
				}
				Pack pack = new Pack(board, flameLife, abs, sh);
			}

			double hasamiThreshold = 1;

			// int x = -1, y = -1;
			if (me == 11) {
				// �E��_��
				int x2 = 9;
				for (int y2 : new int[] { 4, 5, 6 }) {
					int type = map.getType(x2, y2);
					int type2 = map.getType(x2, y2 - 1);
					int power2 = map.getPower(x2, y2 - 1);

					if (type == Constant.Wood) {
						if (type2 == Constant.Passage || type2 == Constant.Flames || type2 == Constant.Bomb) {
							// (x2, y2-1)�Ɍ������B
							double ddd = dis2.data[x2][y2 - 1];
							if (ddd < 100) {
								int dir = BBMUtility.ComputeFirstDirection(dis2, x2, y2 - 1);
								double score = safetyScoreWorst[me - 10][dir];
								if (score > hasamiThreshold) {
									actionFinal = dir;
									break;
								} else {
									double scoreStop = safetyScoreWorst[me - 10][0];
									if (scoreStop > hasamiThreshold) {
										actionFinal = 0;
										reason = "���݌������[�u";
									}
								}
							}
						} else if (type2 == me) {
							// ���ɖړI�̏ꏊ�ɂ�����B
							if (power2 == 0 && abs[me - 10].numBombHold > 0) {
								// ���e���ݒu����ĂȂ�������A���e��ݒu����B
								double score = safetyScoreWorst[me - 10][5];
								if (score > hasamiThreshold) {
									actionFinal = 5;
									reason = "���݌������[�u";
									break;
								}
							}
						}
					}
				}
			} else if (me == 13) {
				// ����_���B
				int y2 = 9;
				for (int x2 : new int[] { 4, 5, 6 }) {
					int type = map.getType(x2, y2);
					int type2 = map.getType(x2 - 1, y2);
					int power2 = map.getPower(x2 - 1, y2);

					if (type == Constant.Wood) {
						if (type2 == Constant.Passage || type2 == Constant.Flames || type2 == Constant.Bomb) {
							// (x2, y2-1)�Ɍ������B
							double ddd = dis2.data[x2 - 1][y2];
							if (ddd < 100) {
								int dir = BBMUtility.ComputeFirstDirection(dis2, x2 - 1, y2);
								double score = safetyScoreWorst[me - 10][dir];
								if (score > hasamiThreshold) {
									actionFinal = dir;
									break;
								} else {
									double scoreStop = safetyScoreWorst[me - 10][0];
									if (scoreStop > hasamiThreshold) {
										actionFinal = 0;
										reason = "���݌������[�u";
									}
								}
							}
						} else if (type2 == me) {
							// ���ɖړI�̏ꏊ�ɂ�����B
							if (power2 == 0 && abs[me - 10].numBombHold > 0) {
								// ���e���ݒu����ĂȂ�������A���e��ݒu����B
								double score = safetyScoreWorst[me - 10][5];
								if (score > hasamiThreshold) {
									actionFinal = 5;
									reason = "���݌������[�u";
									break;
								}
							}
						}
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �G��������֌������B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (actionFinal == -1 && numVisibleEnemy == 0 && numAliveEnemy == 2) {
			double dddmin = Double.POSITIVE_INFINITY;
			int dirmin = -1;
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;
				LastAgentPosition lap = laps[ai];
				// �ړI�n�i�Ō�ɓG�������ꏊ�j���A���Ɋϑ��͈͂ɂ���ꍇ�́A�Ӗ��Ȃ��B
				int isInsideVisibleArea = Math.abs(lap.x - agentMe.x) + Math.abs(lap.y - agentMe.y);
				if (isInsideVisibleArea <= 4) continue;
				double ddd = dis.data[lap.x][lap.y];
				if (ddd > 100) continue;
				int action = BBMUtility.ComputeFirstDirection(dis, lap.x, lap.y);
				double score = safetyScoreWorst[me - 10][action];
				if (score < usualMoveThreshold) continue;
				if (ddd < dddmin) {
					dddmin = ddd;
					dirmin = action;
				}
			}

			if (dirmin != -1) {
				actionFinal = dirmin;
				reason = "�G��������֌��������[�u";
			}
		}

		// �F�B��������֌������B
		if (false) {
			// if (actionFinal == -1 && numVisibleEnemy == 0 && numAliveEnemy == 2) {
			double dddmin = Double.POSITIVE_INFINITY;
			int dirmin = -1;
			for (int ai = 0; ai < 4; ai++) {
				if (ai != friend - 10) continue;
				LastAgentPosition lap = laps[ai];
				// �ړI�n�i�Ō�ɓG�������ꏊ�j���A���Ɋϑ��͈͂ɂ���ꍇ�́A�Ӗ��Ȃ��B
				int isInsideVisibleArea = Math.abs(lap.x - agentMe.x) + Math.abs(lap.y - agentMe.y);
				if (isInsideVisibleArea <= 4) continue;
				double ddd = dis.data[lap.x][lap.y];
				if (ddd > 100) continue;
				int action = BBMUtility.ComputeFirstDirection(dis, lap.x, lap.y);
				double score = safetyScoreWorst[me - 10][action];
				if (score < usualMoveThreshold) continue;
				if (ddd < dddmin) {
					dddmin = ddd;
					dirmin = action;
				}
			}

			if (dirmin != -1) {
				actionFinal = dirmin;
				reason = "������������֌��������[�u";
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �l�߂����Ԃł���΁A�l�߂�B
		// TODO �l�߂�v�����N�Ȃ̂���Ԃ��悤�ɂ������B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		if (false) {
			// if (actionFinal == -1) {
			Pack packNow;
			if (true) {
				Ability[] abs2 = new Ability[4];
				for (int ai = 0; ai < 4; ai++) {
					abs2[ai] = new Ability(abs[ai]);
					if (ai + 10 == me) continue;
					abs2[ai].kick = true;
					abs2[ai].numMaxBomb = 3;
					abs2[ai].numBombHold = 3;
					if (abs2[ai].strength_fix == -1) {
						abs2[ai].strength = maxPower;
					} else {
						abs2[ai].strength = abs2[ai].strength_fix;
					}
				}

				StatusHolder sh = new StatusHolder();
				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						int type = map.getType(x, y);
						if (Constant.isAgent(type)) {
							sh.setAgent(x, y, type);
						}
					}
				}

				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						Node node = bombMap[x][y];
						if (node == null) continue;
						// TODO �Ƃ肠������~���Ă锚�e�����l������B
						if (node.dirs[0] == false) continue;
						sh.setBomb(x, y, -1, node.life, 0, node.power);
					}
				}

				packNow = new Pack(map.board, flameLife, abs2, sh);
			}

			int killScoreMin = Integer.MAX_VALUE;
			int aiTarget = -1;
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;

				int killScore = KillScoreEvaluator.computeKillScore(packNow, ai);
				if (killScore < Integer.MAX_VALUE) {
					killScoreMin = killScore;
					aiTarget = ai;
					break;
				}
			}

			if (aiTarget != -1) {
				ForwardModel fm = new ForwardModel();
				int amin = -1;
				for (int a = 0; a < 6; a++) {
					double scoreMe = safetyScoreWorst[me - 10][a];
					if (scoreMe < attackThreshold) continue;
					int[] actions = new int[4];
					actions[me - 10] = a;
					Pack packNext = fm.Step(collapse, frame, packNow.board, packNow.flameLife, packNow.abs, packNow.sh, actions);
					int temp = KillScoreEvaluator.computeKillScore(packNext, aiTarget);
					if (temp < killScoreMin) {
						killScoreMin = temp;
						amin = a;
					}
				}
				if (amin != -1) {
					actionFinal = amin;
					reason = "���l�߂�B";
					System.out.println("���l�߂�B�I�I�I" + actionFinal);
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �����̈��S���m�ۂ�����ԂŁA������댯�ɂł���P�[�X��T���B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// �y�A����
		if (false) {
			// if (actionFinal == -1 && numVisibleTeam == 2 && numVisibleEnemy > 0) {
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;
				double min = Double.POSITIVE_INFINITY;
				double max = 0;
				for (int a = 0; a < 6; a++) {
					for (int b = 0; b < 6; b++) {
						double num1 = sr.pairScore[a][b][me - 10].num;
						double num2 = sr.pairScore[a][b][friend - 10].num;
						double num3 = sr.pairScore[a][b][ai].num;
						if (num1 == 0 || num2 == 0 || num3 == 0) continue;

						double s1 = sr.pairScore[a][b][me - 10].min;
						double s2 = sr.pairScore[a][b][friend - 10].min;
						double s3 = sr.pairScore[a][b][ai].sum / num3;

						if (s1 < attackThreshold || s2 < attackThreshold) continue;

						if (s3 < min) min = s3;
						if (s3 > max) max = s3;
					}
				}

				// �����̃A�N�V����������e����^���Ȃ���Δ�΂��B
				if (min == max) continue;

				if (min != Double.POSITIVE_INFINITY) {

					int besta = -1;
					int bestb = -1;

					ArrayList<Integer> set = new ArrayList<Integer>();
					for (int a = 0; a < 6; a++) {
						for (int b = 0; b < 6; b++) {
							double num1 = sr.pairScore[a][b][me - 10].num;
							double num2 = sr.pairScore[a][b][friend - 10].num;
							double num3 = sr.pairScore[a][b][ai].num;
							if (num1 == 0 || num2 == 0 || num3 == 0) continue;

							double s1 = sr.pairScore[a][b][me - 10].min;
							double s2 = sr.pairScore[a][b][friend - 10].min;
							double s3 = sr.pairScore[a][b][ai].sum / num3;

							if (s1 < attackThreshold || s2 < attackThreshold) continue;

							if (s3 == min) {
								set.add(a);
								besta = a;
								bestb = b;
							}
						}
					}
					int index = rand.nextInt(set.size());
					actionFinal = set.get(index);
					reason = "�U������B1���B�y�A�B";

					if (min < 0.1) {
						System.out.println("chance?");
					}

					break;
				}
			}
		}

		// �P�ƈ���
		if (false) {
			// if (actionFinal == -1 && numVisibleEnemy > 0) {
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;
				double min = Double.POSITIVE_INFINITY;
				double max = 0;
				for (int a = 0; a < 6; a++) {
					double scoreMe = safetyScoreWorst[me - 10][a];
					double scoreEnemy = safetyScoreAverage[ai][a];
					if (Double.isNaN(scoreEnemy)) continue;
					if (scoreMe < attackThreshold) continue;
					if (scoreMe < scoreEnemy) continue;
					if (scoreEnemy < min) {
						min = scoreEnemy;
					}
					if (scoreEnemy > max) {
						max = scoreEnemy;
					}
				}

				// �����̃A�N�V����������e����^���Ȃ���Δ�΂��B
				if (min == max) continue;

				if (min != Double.POSITIVE_INFINITY) {
					ArrayList<Integer> set = new ArrayList<Integer>();
					for (int a = 0; a < 6; a++) {
						double scoreMe = safetyScoreWorst[me - 10][a];
						double scoreEnemy = safetyScoreAverage[ai][a];
						if (Double.isNaN(scoreEnemy)) continue;
						if (scoreMe < attackThreshold) continue;
						if (scoreMe < scoreEnemy) continue;
						if (scoreEnemy == min) {
							set.add(a);
						}
					}
					int index = rand.nextInt(set.size());
					actionFinal = set.get(index);
					reason = "�U������B1���B";
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �A�C�e��������Ȃ�A�A�C�e�������ɍs���B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (actionFinal == -1) {
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
				double score = safetyScoreWorst[me - 10][action];
				if (score > usualMoveThreshold) {
					actionFinal = action;
					reason = "�V�K�B�A�C�e������邽�߂Ɉړ�����B";
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �؂�����Ȃ�A�؂��󂵂ɍs���B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (actionFinal == -1) {
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

			if (actionFinal == -1 && mindis == 0) {
				// Wood���󂹂�ʒu�ɂ�����A���e��u���B
				if (ab.numBombHold > 0) {
					int action = 5;
					double score = safetyScoreWorst[me - 10][action];
					if (score > usualMoveThreshold) {
						actionFinal = action;
						reason = "�V�K�B�؂��󂷂��߂ɔ��e��ݒu����B";
					}
				}
			}

			if (actionFinal == -1 && mindis < Integer.MAX_VALUE && mindis > 0) {
				// Wood���󂹂�ꏊ���݂�������A�������Ɉړ�����B
				int action = BBMUtility.ComputeFirstDirection(dis, minx, miny);
				double score = safetyScoreWorst[me - 10][action];
				if (score > usualMoveThreshold) {
					actionFinal = action;
					reason = "�V�K�B�؂��󂷂��߂Ɉړ�����B";
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �ŋߌ��ĂȂ��ꏊ�Ɉړ����Ă݂�B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (actionFinal == -1 && numVisibleEnemy == 0) {

			double frameOld = Double.POSITIVE_INFINITY;
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					double ddd = dis.data[x][y];
					if (ddd == Double.MAX_VALUE) continue;
					double fff = lastLook.data[x][y];
					if (fff < frameOld) {
						frameOld = fff;
					}
				}
			}

			double frameDelta = frame - frameOld;

			if (frameDelta > 10) {
				List<int[]> posList = new ArrayList<int[]>();
				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						double ddd = dis.data[x][y];
						if (ddd == Double.MAX_VALUE) continue;
						double fff = lastLook.data[x][y];
						if (fff == frameOld) {
							int[] pos = new int[] { x, y };
							posList.add(pos);
						}
					}
				}

				for (int i = 0; i < 100; i++) {
					int[] pos = posList.get(rand.nextInt(posList.size()));
					int x = pos[0];
					int y = pos[1];
					int action = BBMUtility.ComputeFirstDirection(dis, x, y);
					double score = safetyScoreWorst[me - 10][action];
					if (score < usualMoveThreshold) continue;
					actionFinal = action;
					reason = "�ŋߌ��Ă��Ȃ��ꏊ�����ɍs���B";
					break;
				}
			}
		}

		if (mugenLoopRecoveryKikan > 0) {
			mugenLoopRecoveryKikan--;
			actionFinal = -1;
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// ��̑I�����Ŋ댯�ȏ�ԂɊׂ�Ȃ�A�����Ƃ����S�ȃA�N�V���������B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// �y�A����
		if (false) {
			// if (actionFinal == -1 && numVisibleTeam == 2) {
			double max = 0;
			int maxa = -1;
			int maxb = -1;
			for (int a = 0; a < 6; a++) {
				for (int b = 0; b < 6; b++) {
					double numMe = sr.pairScore[a][b][me - 10].num;
					double numFriend = sr.pairScore[a][b][friend - 10].num;
					if (numMe == 0 || numFriend == 0) continue;
					double scoreMe = sr.pairScore[a][b][me - 10].min;
					double scoreFriend = sr.pairScore[a][b][friend - 10].min;
					double scoreMeAve = sr.pairScore[a][b][me - 10].sum / numMe;
					double scoreFriendAve = sr.pairScore[a][b][friend - 10].sum / numFriend;
					double score = (scoreMe + scoreMeAve * 1.0e-10) * (scoreFriend + scoreFriendAve * 1.0e-10);
					if (score > max) {
						max = score;
						maxa = a;
						maxb = b;
					}
				}
			}
			if (maxa != -1) {
				actionFinal = maxa;
				reason = "�����Ƃ����S�ȃA�N�V������I�ԁB�y�A�����B";
			} else {
				for (int a = 0; a < 6; a++) {
					for (int b = 0; b < 6; b++) {
						double numMe = sr.pairScore[a][b][me - 10].num;
						double numFriend = sr.pairScore[a][b][friend - 10].num;
						if (numMe == 0 || numFriend == 0) continue;
						double scoreMe = sr.pairScore[a][b][me - 10].sum / numMe;
						double scoreFriend = sr.pairScore[a][b][friend - 10].sum / numFriend;
						double score = scoreMe * scoreFriend;
						if (score > max) {
							max = score;
							maxa = a;
							maxb = b;
						}
					}
				}

				if (maxa != -1) {
					actionFinal = maxa;
					reason = "�����Ƃ����S�ȃA�N�V������I�ԁB�y�A�����B";
				}
			}
		}

		double averageWeight = 0.001;

		// �y�A�����B����Ƃ̍����B
		if (actionFinal == -1 && numVisibleTeam == 2 && numVisibleEnemy > 0) {
			double min = Double.POSITIVE_INFINITY;
			int mina = -1;
			int minb = -1;

			for (int a = 0; a < 6; a++) {
				for (int b = 0; b < 6; b++) {

					double totalEnemy = 1;
					double counterEnemy = 0;
					for (int ai = 0; ai < 4; ai++) {
						if (ai == me - 10) continue;
						if (ai == friend - 10) continue;
						double num = sr.pairScore[a][b][ai].num;
						double sum = sr.pairScore[a][b][ai].sum;
						if (num == 0) continue;
						double sss = sum / num;
						totalEnemy *= sss;
						counterEnemy++;
					}
					if (counterEnemy == 0) continue;
					double scoreEnemy = Math.pow(totalEnemy, 1 / counterEnemy);
					scoreEnemy += 1.0e-20;

					double totalTeam = 1;
					double counterTeam = 0;
					for (int ai : new int[] { me - 10, friend - 10 }) {
						double snum = sr.pairScore[a][b][ai].num;
						double ssum = sr.pairScore[a][b][ai].sum;
						double smin = sr.pairScore[a][b][ai].min;
						double save = ssum / snum;
						double sss = smin + averageWeight * save;
						totalTeam *= sss;
						counterTeam++;
					}

					double scoreTeam = Math.pow(totalTeam, 1 / counterTeam);
					if (scoreTeam == 0) continue;

					double scoreRate = scoreEnemy / scoreTeam;
					if (scoreRate < min) {
						min = scoreRate;
						mina = a;
						minb = b;
					}
				}
			}
			if (mina != -1) {
				actionFinal = mina;
				reason = "�����Ƃ����S�ȃA�N�V������I�ԁB�y�A�����B����";
			}
		}

		// �P�Ɣ����B����Ƃ̍����B
		if (actionFinal == -1 && numVisibleTeam == 1 && numVisibleEnemy > 0) {
			double min = Double.POSITIVE_INFINITY;
			int mina = -1;
			mina = -1;

			for (int a = 0; a < 6; a++) {
				double totalEnemy = 1;
				double counterEnemy = 0;
				for (int ai = 0; ai < 4; ai++) {
					if (ai == me - 10) continue;
					if (ai == friend - 10) continue;
					double sss = safetyScoreAverage[ai][a];
					if (Double.isNaN(sss)) continue;
					totalEnemy *= sss;
					counterEnemy++;
				}
				if (counterEnemy == 0) continue;
				double scoreEnemy = Math.pow(totalEnemy, 1 / counterEnemy);
				scoreEnemy += 1.0e-20;

				double totalTeam = 1;
				double counterTeam = 0;
				for (int ai : new int[] { me - 10, friend - 10 }) {
					double save = safetyScoreAverage[ai][a];
					double smin = safetyScoreWorst[ai][a];
					if (Double.isNaN(save) || Double.isNaN(smin)) continue;
					double sss = smin + averageWeight * save;
					totalTeam *= sss;
					counterTeam++;
				}
				double scoreTeam = Math.pow(totalTeam, 1 / counterTeam);
				if (scoreTeam == 0) continue;

				double scoreRate = scoreEnemy / scoreTeam;
				if (scoreRate < min) {
					min = scoreRate;
					mina = a;
				}
			}

			if (mina != -1) {
				actionFinal = mina;
				reason = "�����Ƃ����S�ȃA�N�V������I�ԁB�P�ƁB����";
			}
		}

		// �P�Ɣ���
		// if(false) {
		if (actionFinal == -1) {
			int actionSelected = findMostSafetyAction(safetyScoreWorst[me - 10], safetyScoreWorst[friend - 10], actionNG);
			System.out.println("step1");
			if (actionSelected == -1) {
				actionSelected = findMostSafetyAction(safetyScoreWorst[me - 10], safetyScoreWorst[friend - 10], -1);
				System.out.println("step2");
				if (actionSelected == -1) {
					actionSelected = findMostSafetyAction(safetyScoreWorst[me - 10], safetyScoreAverage[friend - 10], -1);
					System.out.println("step3");
					if (actionSelected == -1) {
						actionSelected = findMostSafetyAction(safetyScoreWorst[me - 10], null, -1);
						System.out.println("step4");
						if (actionSelected == -1) {
							actionSelected = findMostSafetyAction(safetyScoreAverage[me - 10], safetyScoreWorst[friend - 10], -1);
							System.out.println("step5");
							if (actionSelected == -1) {
								actionSelected = findMostSafetyAction(safetyScoreAverage[me - 10], safetyScoreAverage[friend - 10], -1);
								System.out.println("step6");
								if (actionSelected == -1) {
									actionSelected = findMostSafetyAction(safetyScoreAverage[me - 10], null, -1);
									System.out.println("step7");
									if (safetyScoreAverage[me - 10][0] > 0) {
										actionFinal = rand.nextInt(6);
										reason = "�S�A�N�V�����ňႢ�Ȃ�";
									} else {
										System.out.println("���S�Ɏ��ʁB");
									}
								}
							}
						}
					}
				}
			}

			if (actionSelected != -1) {
				actionFinal = actionSelected;
				reason = "�����Ƃ����S�ȃA�N�V������I�ԁB";
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �����铹���Ȃ��B�G�𓹘A��ɂł���A�N�V����������΁A���s����B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (actionFinal == -1) {
			double min = Double.POSITIVE_INFINITY;
			int mina = -1;

			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;

				for (int a = 0; a < 6; a++) {
					double sss = safetyScoreAverage[ai][a];
					if (Double.isNaN(sss)) continue;
					double sss2 = safetyScoreWorst[friend - 10][a];
					if (Double.isNaN(sss2) == false && sss2 == 0) continue;

					if (sss < min) {
						min = sss;
						mina = a;
					}
				}
			}

			if (mina != -1) {
				actionFinal = mina;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �Ō�̎�i�B�����B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (actionFinal == -1) {
			actionFinal = rand.nextInt(6);
		}

		System.out.println("ComputeOptimalAction: agentID=" + me);
		String line = String.format("ComputeOptimalAction: %s, action=%d", reason, actionFinal);
		System.out.println(line);

		actionLog[actionLogIndex] = actionFinal;
		actionLogIndex = (actionLogIndex + 1) % actionLog.length;

		return actionFinal;
	}

	private int findMostSafetyAction(double[] scoresMe, double[] scoresFriend, int actionNG) {

		double usualMoveThreshold = param.usualThreshold;

		boolean friendDependency = false;
		if (scoresFriend != null) {
			double max = Double.NEGATIVE_INFINITY;
			double min = Double.POSITIVE_INFINITY;
			for (int action = 0; action < 6; action++) {
				double s = scoresFriend[action];
				if (s > max) max = s;
				if (s < min) min = s;
			}
			if (min == max) friendDependency = false;
			else friendDependency = true;
		}

		int actionSelected = -1;
		{
			double max = Double.NEGATIVE_INFINITY;
			double min = Double.POSITIVE_INFINITY;
			for (int action = 0; action < 6; action++) {
				if (action == actionNG) continue;
				double scoreMe = scoresMe[action];
				if (friendDependency) {
					double scoreFriend = scoresFriend[action];
					// if (scoreFriend == 0) continue;
					if (scoreFriend < usualMoveThreshold) continue;
				}
				// if (scoreMe > usualMoveThreshold) scoreMe = usualMoveThreshold;
				if (scoreMe > max) max = scoreMe;
				if (scoreMe < min) min = scoreMe;

			}
			if (max == min) return -1;

			List<Integer> set = new ArrayList<Integer>();
			for (int action = 0; action < 6; action++) {
				if (action == actionNG) continue;
				double scoreMe = scoresMe[action];
				if (friendDependency) {
					double scoreFriend = scoresFriend[action];
					// if (scoreFriend == 0) continue;
					if (scoreFriend < usualMoveThreshold) continue;
				}
				// if (scoreMe > usualMoveThreshold) scoreMe = usualMoveThreshold;
				if (scoreMe == max) {
					set.add(action);
				}
			}
			if (set.size() == 0) return -1;
			actionSelected = set.get(rand.nextInt(set.size()));
		}
		return actionSelected;
	}

}
