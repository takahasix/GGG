/********************************************
 * (C) Copyright IBM Corp. 2018
 ********************************************/
package com.ibm.trl.BBM.mains;

import java.util.Arrays;
import java.util.Random;

import com.ibm.trl.BBM.mains.Agent.Ability;
import com.ibm.trl.BBM.mains.Agent.ModelParameter;
import com.ibm.trl.BBM.mains.ForwardModel.Pack;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;
import com.ibm.trl.BBM.mains.StatusHolder.BombEEE;

import ibm.ANACONDA.Core.MyMatrix;

public class WorstScoreEvaluatorSingle {

	static final int INSTRUCTION_BOARD = 0;
	static final int INSTRUCTION_STAY = 1;
	static final int INSTRUCTION_ALLMOVE = 2;
	static final int INSTRUCTION_EXMAP = 3;

	static final Random rand = new Random();
	static final int numField = GlobalParameter.numField;

	ForwardModel fm = new ForwardModel();
	ModelParameter param;

	// TODO
	double timeDecayRate = 3;

	public WorstScoreEvaluatorSingle(ModelParameter param) {
		this.param = param;
	}

	private MyMatrix[][] computeAgentSteps(Pack[] packsNA, int[][] instructions, MyMatrix[][] exmaps, int numFirstMoveStepsAsFriend) throws Exception {

		int numt = packsNA.length;

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// 仲間エージェントの動き。最初の位置から動かない。
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		MyMatrix[][] stepMaps = new MyMatrix[numt][4];
		for (int ai = 0; ai < 4; ai++) {
			for (int t = 0; t < numt; t++) {
				stepMaps[t][ai] = new MyMatrix(numField, numField, numt);
			}
		}

		for (int ai = 0; ai < 4; ai++) {
			Pack packNow = packsNA[0];
			AgentEEE agentNow = packNow.sh.getAgent(ai + 10);
			if (agentNow == null) continue;
			stepMaps[0][ai].data[agentNow.x][agentNow.y] = 0;
		}

		for (int ai = 0; ai < 4; ai++) {
			for (int t = 0; t < numt - 1; t++) {
				int instruction = instructions[t][ai];
				Pack packNow = packsNA[t];
				Pack packNext = packsNA[t + 1];

				if (t >= numFirstMoveStepsAsFriend) {
					instruction = INSTRUCTION_STAY;
				}

				if (instruction == INSTRUCTION_BOARD) {
					AgentEEE agentNow = packNow.sh.getAgent(ai + 10);
					AgentEEE agentNext = packNext.sh.getAgent(ai + 10);
					if (agentNow == null || agentNext == null) continue;
					int x = agentNow.x;
					int y = agentNow.y;
					double stepNow = stepMaps[t][ai].data[x][y];
					int x2 = agentNext.x;
					int y2 = agentNext.y;

					if (true) {
						double stepNext;
						if (x == x2 && y == y2) {
							stepNext = stepNow;
						} else {
							// TODO どっちがいいのだろうか？
							// stepNext = stepNow + 1;
							stepNext = t + 1;
						}
						stepMaps[t + 1][ai].data[x2][y2] = stepNext;
					}
				} else if (instruction == INSTRUCTION_EXMAP) {
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							double flag = exmaps[t + 1][ai].data[x][y];
							if (flag > 0) {
								double stepNow = stepMaps[t][ai].data[x][y];
								double stepNext = stepNow;
								if (stepNext == numt) {
									// TODO どっちがいいのだろうか？
									// stepNext = stepNow + 1;
									stepNext = t + 1;
								}
								stepMaps[t + 1][ai].data[x][y] = stepNext;
							}
						}
					}
				} else if (instruction == INSTRUCTION_STAY) {
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							double stepNow = stepMaps[t][ai].data[x][y];
							if (stepNow == numt) continue;

							if (true) {
								double stepNext = stepNow;
								stepMaps[t + 1][ai].data[x][y] = stepNext;
							}
						}
					}
				} else if (instruction == INSTRUCTION_ALLMOVE) {
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							double stepNow = stepMaps[t][ai].data[x][y];
							if (stepNow == numt) continue;

							// 遷移先の数を数える。
							boolean[] able = new boolean[5];
							for (int[] vec : GlobalParameter.onehopList) {
								int dir = vec[0];
								int dx = vec[1];
								int dy = vec[2];
								int x2 = x + dx;
								int y2 = y + dy;
								if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) continue;
								int type = (int) packNext.board.data[x2][y2];
								if (Constant.isWall(type)) continue;
								if (dir != 0 && type == Constant.Bomb) continue;
								if (type == Constant.Flames) continue;
								able[dir] = true;
							}

							for (int[] vec : GlobalParameter.onehopList) {
								int dir = vec[0];
								int dx = vec[1];
								int dy = vec[2];
								int x2 = x + dx;
								int y2 = y + dy;
								if (able[dir] == false) continue;

								if (true) {
									double stepNext;
									if (dir == 0) {
										stepNext = stepNow;
									} else {
										// TODO どっちがいいのだろうか？
										// stepNext = stepNow + 1;
										stepNext = t + 1;
									}
									if (stepNext < stepMaps[t + 1][ai].data[x2][y2]) {
										stepMaps[t + 1][ai].data[x2][y2] = stepNext;
									}
								}
							}
						}
					}

					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							int type = (int) packNext.board.data[x][y];
							if (type == Constant.Flames) {
								stepMaps[t + 1][ai].data[x][y] = numt;
							}
						}
					}
				}
			}
		}

		return stepMaps;
	}

	public double[][] Do3(boolean collapse, int frame, int me, int friend, Ability[] abs, Pack[] packsOrg, MyMatrix[][] exmaps, int[][] instructions) throws Exception {

		// System.out.println("aaa");

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// 基本変数の作成
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		int numt = instructions.length;
		int numFirstMoveStepsAsFriend = param.numFirstMoveStepsAsFriend;
		double gainOffset = param.gainOffset;

		int[] teamNumber = new int[4];
		teamNumber[me - 10] = 1;
		teamNumber[friend - 10] = 1;

		double rateLevel = param.rateLevelDouble;
		if (true) {
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

			if (numAliveEnemy == 2) {
				rateLevel = param.rateLevelDouble;
			} else {
				rateLevel = param.rateLevelSingle;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// numtステップ先まで盤面をシミュレーションしておく。
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		Pack[] packs = new Pack[numt];
		for (int t = 0; t < packsOrg.length; t++) {
			packs[t] = packsOrg[t];
		}

		Pack[] packsNA = new Pack[numt];
		{
			packsNA[0] = packs[0];
			for (int t = 1; t < numt; t++) {
				if (packs[t] != null) {
					// 元系列が存在するときは、そのままコピーする。
					packsNA[t] = packs[t];
				} else {
					// 元系列が存在しないときは、ステップ計算する。ステップ計算では、エージェントはすべて消す。
					Pack packPreNA;
					if (packs[t - 1] == null) {
						packPreNA = packsNA[t - 1];
					} else {
						Pack packPre = packs[t - 1];
						packPreNA = new Pack(packPre);
						for (int ai = 0; ai < 4; ai++) {
							packPreNA.removeAgent(ai + 10);
						}
					}
					int[] actions = new int[4];
					Pack packNextNA = fm.Step(collapse, frame + t - 1, packPreNA, actions);
					packsNA[t] = packNextNA;
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// エージェントを動かして、存在確率を計算する。
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 仲間エージェントの動き。最初の位置から数ステップだけ動く。
		MyMatrix[][] stepMaps_stay = computeAgentSteps(packsNA, instructions, exmaps, numFirstMoveStepsAsFriend);

		// 敵エージェントの動き
		MyMatrix[][] stepMaps_move = computeAgentSteps(packsNA, instructions, exmaps, Integer.MAX_VALUE);

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// 全行動の経路のスコアを計算してみる。
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		MyMatrix[][] hitNearestStep = new MyMatrix[numt][4];
		for (int ai = 0; ai < 4; ai++) {
			for (int t = 0; t < numt; t++) {
				hitNearestStep[t][ai] = new MyMatrix(numField, numField, Double.NEGATIVE_INFINITY);
			}
		}

		for (int ai = 0; ai < 4; ai++) {
			Pack packNow = packsNA[0];
			AgentEEE agentNow = packNow.sh.getAgent(ai + 10);
			if (agentNow == null) continue;
			hitNearestStep[0][ai].data[agentNow.x][agentNow.y] = numt;
		}

		for (int ai = 0; ai < 4; ai++) {

			Pack packInit = packsNA[0];
			AgentEEE agentInit = packInit.sh.getAgent(ai + 10);
			if (agentInit == null) continue;

			for (int t = 0; t < numt - 1; t++) {
				int instruction = instructions[t][ai];
				Pack packNow = packsNA[t];
				Pack packNext = packsNA[t + 1];

				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {

						double nearestStepNow = hitNearestStep[t][ai].data[x][y];
						if (nearestStepNow == Double.NEGATIVE_INFINITY) continue;

						boolean[] able = new boolean[5];
						for (int[] vec : GlobalParameter.onehopList) {
							int dir = vec[0];
							int dx = vec[1];
							int dy = vec[2];
							int x2 = x + dx;
							int y2 = y + dy;
							if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) continue;
							if (instruction == INSTRUCTION_BOARD) {
								AgentEEE agentNext = packNext.sh.getAgent(ai + 10);
								if (agentNext == null) continue;
								if (x2 != agentNext.x || y2 != agentNext.y) continue;
							} else if (instruction == INSTRUCTION_EXMAP) {
								double flag = exmaps[t + 1][ai].data[x2][y2];
								if (flag == 0) continue;
							} else if (instruction == INSTRUCTION_STAY) {
								if (dx != 0 || dy != 0) continue;
							} else if (instruction == INSTRUCTION_ALLMOVE) {
							}
							int type = (int) packNext.board.data[x2][y2];
							if (Constant.isWall(type)) continue;
							if (type == Constant.Flames) continue;
							if (dir != 0 && type == Constant.Bomb) continue;
							// 爆弾とのクロス判定を入れておく。
							if (dir != 0) {
								BombEEE bbb1 = packNow.sh.getBomb(x2, y2);
								BombEEE bbb2 = packNext.sh.getBomb(x, x);
								if (bbb1 != null && bbb2 != null) {
									if (bbb1.power == bbb2.power && bbb1.life - 1 == bbb2.life) {
										if (dir == 1) {
											if (bbb1.dir == 2) continue;
										} else if (dir == 2) {
											if (bbb1.dir == 1) continue;
										} else if (dir == 3) {
											if (bbb1.dir == 4) continue;
										} else if (dir == 4) {
											if (bbb1.dir == 3) continue;
										}
									}
								}
							}
							able[dir] = true;
						}

						for (int[] vec : GlobalParameter.onehopList) {
							int dir = vec[0];
							int dx = vec[1];
							int dy = vec[2];
							int x2 = x + dx;
							int y2 = y + dy;
							if (able[dir] == false) continue;

							double nearestStepNext = nearestStepNow;
							for (int ai2 = 0; ai2 < 4; ai2++) {
								if (ai2 == ai) continue;

								MyMatrix stepMapNext = stepMaps_move[t + 1][ai2];
								MyMatrix stepMapNow = stepMaps_move[t][ai2];
								if (teamNumber[ai] == teamNumber[ai2]) {
									stepMapNext = stepMaps_stay[t + 1][ai2];
									stepMapNow = stepMaps_stay[t][ai2];
								}

								double stepNext = stepMapNext.data[x2][y2];
								if (stepNext != numt) {
									if (stepNext < nearestStepNext) {
										nearestStepNext = stepNext;
									}
								} else {
									double stepCross1 = stepMapNow.data[x2][y2];
									double stepCross2 = stepMapNext.data[x][y];
									if (stepCross1 != numt && stepCross2 != numt) {
										if (stepCross2 < nearestStepNext) {
											nearestStepNext = Math.min(stepCross1, stepCross2);
										}
									}
								}
							}
							if (nearestStepNext > hitNearestStep[t + 1][ai].data[x2][y2]) {
								hitNearestStep[t + 1][ai].data[x2][y2] = nearestStepNext;
							}
						}
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// 盤面系列からスコアを計算する。
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		double[][] scores = new double[4][2];

		// 縮小断面積
		if (false) {
			for (int ai = 0; ai < 4; ai++) {

				Pack packInit = packsNA[0];
				AgentEEE agentInit = packInit.sh.getAgent(ai + 10);
				if (agentInit == null) continue;

				int t = numt - 1;
				MyMatrix mat = hitNearestStep[t][ai];
				double[] gain = new double[numt + 1];
				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						if (mat.data[x][y] < 0) continue;
						int step = (int) mat.data[x][y];
						gain[step]++;
					}
				}

				for (int s = 0; s < numt; s++) {
					gain[s] = gain[s] - gainOffset;
					if (gain[s] < 0) gain[s] = 0;
				}

				double total = 0;
				for (int s = 0; s < numt + 1; s++) {
					total += Math.pow(rateLevel, s - numt) * gain[s];
				}

				scores[ai][0] += total;
				scores[ai][1] += 1;
			}
		}

		// 縮小断面積、全時間考慮
		if (true) {
			double rate = timeDecayRate;

			for (int ai = 0; ai < 4; ai++) {
				Pack packInit = packsNA[0];
				AgentEEE agentInit = packInit.sh.getAgent(ai + 10);
				if (agentInit == null) continue;

				double totaltotal = 0;
				double weighttotal = 0;
				// for (int t = 1; t < numt; t++) {
				for (int t : new int[] { 1, 5, numt - 1 }) {
					// for (int t : new int[] { numt - 1 }) {
					MyMatrix mat = hitNearestStep[t][ai];
					double[] gain = new double[numt + 1];
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							if (mat.data[x][y] < 0) continue;
							int step = (int) mat.data[x][y];
							gain[step]++;
						}
					}

					for (int s = 0; s < numt; s++) {
						gain[s] = gain[s] - gainOffset;
						if (gain[s] < 0) gain[s] = 0;
					}

					double total = 0;
					for (int s = 0; s < numt + 1; s++) {
						total += Math.pow(rateLevel, s - numt) * gain[s];
					}

					double weight = Math.pow(rate, t);
					totaltotal += total * weight;
					weighttotal += weight;
				}

				scores[ai][0] += totaltotal / weighttotal;
				scores[ai][1] += 1;
			}
		}

		return scores;
	}

	/**********************************************************************************************
	 * 
	 * 
	 * HighSpeedバージョン
	 * 
	 * 
	 **********************************************************************************************/
	private double[] computeAgentSteps_HighSpeed(Pack[] packsNA, int[][] instructions, MyMatrix[][] exmaps, int numFirstMoveStepsAsFriend) throws Exception {
		int numt = packsNA.length;

		double[] stepMaps = new double[4 * numt * numField * numField];
		Arrays.fill(stepMaps, numt);

		for (int ai = 0; ai < 4; ai++) {
			Pack packNow = packsNA[0];
			AgentEEE agentNow = packNow.sh.getAgent(ai + 10);
			if (agentNow == null) continue;
			int x = agentNow.x;
			int y = agentNow.y;
			int t = 0;
			int index = ai * numt * numField * numField + t * numField * numField + y * numField + x;
			stepMaps[index] = 0;
		}

		for (int ai = 0; ai < 4; ai++) {
			for (int t = 0; t < numt - 1; t++) {
				int instruction = instructions[t][ai];
				Pack packNow = packsNA[t];
				Pack packNext = packsNA[t + 1];

				if (t >= numFirstMoveStepsAsFriend) {
					instruction = INSTRUCTION_STAY;
				}

				if (instruction == INSTRUCTION_BOARD) {
					AgentEEE agentNow = packNow.sh.getAgent(ai + 10);
					AgentEEE agentNext = packNext.sh.getAgent(ai + 10);
					if (agentNow == null || agentNext == null) continue;
					int x = agentNow.x;
					int y = agentNow.y;
					int index = ai * numt * numField * numField + t * numField * numField + y * numField + x;
					double stepNow = stepMaps[index];
					int x2 = agentNext.x;
					int y2 = agentNext.y;

					if (true) {
						double stepNext;
						if (x == x2 && y == y2) {
							stepNext = stepNow;
						} else {
							// TODO どっちがいいのだろうか？
							// stepNext = stepNow + 1;
							stepNext = t + 1;
						}
						int index2 = ai * numt * numField * numField + (t + 1) * numField * numField + y2 * numField + x2;
						stepMaps[index2] = stepNext;
					}
				} else if (instruction == INSTRUCTION_EXMAP) {
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							double flag = exmaps[t + 1][ai].data[x][y];
							if (flag > 0) {
								int index = ai * numt * numField * numField + t * numField * numField + y * numField + x;
								double stepNow = stepMaps[index];
								double stepNext = stepNow;
								if (stepNext == numt) {
									// TODO どっちがいいのだろうか？
									// stepNext = stepNow + 1;
									stepNext = t + 1;
								}
								int index2 = ai * numt * numField * numField + (t + 1) * numField * numField + y * numField + x;
								stepMaps[index2] = stepNext;
							}
						}
					}
				} else if (instruction == INSTRUCTION_STAY) {
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							int index = ai * numt * numField * numField + t * numField * numField + y * numField + x;
							double stepNow = stepMaps[index];
							if (stepNow == numt) continue;

							if (true) {
								double stepNext = stepNow;
								int index2 = ai * numt * numField * numField + (t + 1) * numField * numField + y * numField + x;
								stepMaps[index2] = stepNext;
							}
						}
					}
				} else if (instruction == INSTRUCTION_ALLMOVE) {
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							int index = ai * numt * numField * numField + t * numField * numField + y * numField + x;
							double stepNow = stepMaps[index];
							if (stepNow == numt) continue;

							// 遷移先の数を数える。
							boolean[] able = new boolean[5];
							for (int[] vec : GlobalParameter.onehopList) {
								int dir = vec[0];
								int dx = vec[1];
								int dy = vec[2];
								int x2 = x + dx;
								int y2 = y + dy;
								if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) continue;
								int type = (int) packNext.board.data[x2][y2];
								if (Constant.isWall(type)) continue;
								if (dir != 0 && type == Constant.Bomb) continue;
								if (type == Constant.Flames) continue;
								able[dir] = true;
							}

							for (int[] vec : GlobalParameter.onehopList) {
								int dir = vec[0];
								int dx = vec[1];
								int dy = vec[2];
								int x2 = x + dx;
								int y2 = y + dy;
								if (able[dir] == false) continue;

								if (true) {
									double stepNext;
									if (dir == 0) {
										stepNext = stepNow;
									} else {
										// TODO どっちがいいのだろうか？
										// stepNext = stepNow + 1;
										stepNext = t + 1;
									}
									int index2 = ai * numt * numField * numField + (t + 1) * numField * numField + y2 * numField + x2;

									if (stepNext < stepMaps[index2]) {
										stepMaps[index2] = stepNext;
									}
								}
							}
						}
					}
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							int type = (int) packNext.board.data[x][y];
							if (type == Constant.Flames) {
								int index2 = ai * numt * numField * numField + (t + 1) * numField * numField + y * numField + x;
								stepMaps[index2] = numt;
							}
						}
					}
				}
			}
		}

		return stepMaps;
	}

	public double[][] Do3_HighSpeed(boolean collapse, int frame, int me, int friend, Ability[] abs, Pack[] packsOrg, MyMatrix[][] exmaps, int[][] instructions) throws Exception {

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// 基本変数の作成
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		int numt = instructions.length;
		int numFirstMoveStepsAsFriend = param.numFirstMoveStepsAsFriend;
		double gainOffset = param.gainOffset;

		int[] teamNumber = new int[4];
		teamNumber[me - 10] = 1;
		teamNumber[friend - 10] = 1;

		double rateLevel = param.rateLevelDouble;
		if (true) {
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

			if (numAliveEnemy == 2) {
				rateLevel = param.rateLevelDouble;
			} else {
				rateLevel = param.rateLevelSingle;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// numtステップ先まで盤面をシミュレーションしておく。
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		Pack[] packs = new Pack[numt];
		for (int t = 0; t < packsOrg.length; t++) {
			packs[t] = packsOrg[t];
		}

		Pack[] packsNA = new Pack[numt];
		{
			packsNA[0] = packs[0];
			for (int t = 1; t < numt; t++) {
				if (packs[t] != null) {
					// 元系列が存在するときは、そのままコピーする。
					packsNA[t] = packs[t];
				} else {
					// 元系列が存在しないときは、ステップ計算する。ステップ計算では、エージェントはすべて消す。
					Pack packPreNA;
					if (packs[t - 1] == null) {
						packPreNA = packsNA[t - 1];
					} else {
						Pack packPre = packs[t - 1];
						packPreNA = new Pack(packPre);
						for (int ai = 0; ai < 4; ai++) {
							packPreNA.removeAgent(ai + 10);
						}
					}
					int[] actions = new int[4];
					Pack packNextNA = fm.Step(collapse, frame + t - 1, packPreNA, actions);
					packsNA[t] = packNextNA;
				}
			}
		}

		if (collapse && frame >= 490 && frame <= 500) {
			Pack[] packsNA2 = new Pack[numt + 5];
			for (int t = 0; t < numt; t++) {
				packsNA2[t] = packsNA[t];
			}

			Pack packLast = packsNA[numt - 1];
			for (int i = 0; i < 5; i++) {
				packLast = fm.Step(collapse, 570 + i, packLast, new int[4]);
				packsNA2[numt + i] = packLast;
			}

			int[][] instructions2 = new int[numt + 5][4];
			for (int ai = 0; ai < 4; ai++) {
				for (int t = 0; t < numt; t++) {
					instructions2[t][ai] = instructions[t][ai];
				}
				for (int t = numt; t < numt + 5; t++) {
					instructions2[t][ai] = instructions[numt - 1][ai];
				}
			}

			packsNA = packsNA2;
			instructions = instructions2;

			numt = numt + 5;
		}
		if (collapse && frame >= 565 && frame <= 575) {
			Pack[] packsNA2 = new Pack[numt + 5];
			for (int t = 0; t < numt; t++) {
				packsNA2[t] = packsNA[t];
			}

			Pack packLast = packsNA[numt - 1];
			for (int i = 0; i < 5; i++) {
				packLast = fm.Step(collapse, 645 + i, packLast, new int[4]);
				packsNA2[numt + i] = packLast;
			}

			int[][] instructions2 = new int[numt + 5][4];
			for (int ai = 0; ai < 4; ai++) {
				for (int t = 0; t < numt; t++) {
					instructions2[t][ai] = instructions[t][ai];
				}
				for (int t = numt; t < numt + 5; t++) {
					instructions2[t][ai] = instructions[numt - 1][ai];
				}
			}

			packsNA = packsNA2;
			instructions = instructions2;

			numt = numt + 5;
		}
		if (collapse && frame >= 640 && frame <= 650) {
			Pack[] packsNA2 = new Pack[numt + 5];
			for (int t = 0; t < numt; t++) {
				packsNA2[t] = packsNA[t];
			}

			Pack packLast = packsNA[numt - 1];
			for (int i = 0; i < 5; i++) {
				packLast = fm.Step(collapse, 720 + i, packLast, new int[4]);
				packsNA2[numt + i] = packLast;
			}

			int[][] instructions2 = new int[numt + 5][4];
			for (int ai = 0; ai < 4; ai++) {
				for (int t = 0; t < numt; t++) {
					instructions2[t][ai] = instructions[t][ai];
				}
				for (int t = numt; t < numt + 5; t++) {
					instructions2[t][ai] = instructions[numt - 1][ai];
				}
			}

			packsNA = packsNA2;
			instructions = instructions2;

			numt = numt + 5;
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// エージェントを動かして、存在確率を計算する。
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 仲間エージェントの動き。最初の位置から動かない。
		double[] stepMaps_stay = computeAgentSteps_HighSpeed(packsNA, instructions, exmaps, numFirstMoveStepsAsFriend);

		// 敵エージェントの動き
		double[] stepMaps_move = computeAgentSteps_HighSpeed(packsNA, instructions, exmaps, Integer.MAX_VALUE);

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// 全行動の経路のスコアを計算してみる。
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		double[] hitNearestStep = new double[4 * numt * numField * numField];
		Arrays.fill(hitNearestStep, Double.NEGATIVE_INFINITY);

		for (int ai = 0; ai < 4; ai++) {
			{
				Pack packInit = packsNA[0];
				AgentEEE agentInit = packInit.sh.getAgent(ai + 10);
				if (agentInit == null) continue;

				int t = 0;
				int x = agentInit.x;
				int y = agentInit.y;
				int index = ai * numt * numField * numField + t * numField * numField + y * numField + x;
				hitNearestStep[index] = numt;
			}

			for (int t = 0; t < numt - 1; t++) {

				int instruction = instructions[t][ai];
				Pack packNow = packsNA[t];
				Pack packNext = packsNA[t + 1];

				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {

						int index = ai * numt * numField * numField + t * numField * numField + y * numField + x;
						double nearestStepNow = hitNearestStep[index];
						if (nearestStepNow == Double.NEGATIVE_INFINITY) continue;

						boolean[] able = new boolean[5];
						for (int[] vec : GlobalParameter.onehopList) {
							int dir = vec[0];
							int dx = vec[1];
							int dy = vec[2];
							int x2 = x + dx;
							int y2 = y + dy;
							if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) continue;
							if (instruction == INSTRUCTION_BOARD) {
								AgentEEE agentNext = packNext.sh.getAgent(ai + 10);
								if (agentNext == null) continue;
								if (x2 != agentNext.x || y2 != agentNext.y) continue;
							} else if (instruction == INSTRUCTION_STAY) {
								if (dx != 0 || dy != 0) continue;
							} else if (instruction == INSTRUCTION_ALLMOVE) {
							}
							int type = (int) packNext.board.data[x2][y2];
							if (Constant.isWall(type)) continue;
							if (type == Constant.Flames) continue;
							if (dir != 0 && type == Constant.Bomb) continue;
							// 爆弾とのクロス判定を入れておく。
							if (dir != 0) {
								BombEEE bbb1 = packNow.sh.getBomb(x2, y2);
								BombEEE bbb2 = packNext.sh.getBomb(x, x);
								if (bbb1 != null && bbb2 != null) {
									if (bbb1.power == bbb2.power && bbb1.life - 1 == bbb2.life) {
										if (dir == 1) {
											if (bbb1.dir == 2) continue;
										} else if (dir == 2) {
											if (bbb1.dir == 1) continue;
										} else if (dir == 3) {
											if (bbb1.dir == 4) continue;
										} else if (dir == 4) {
											if (bbb1.dir == 3) continue;
										}
									}
								}
							}
							able[dir] = true;
						}

						for (int[] vec : GlobalParameter.onehopList) {
							int dir = vec[0];
							int dx = vec[1];
							int dy = vec[2];
							int x2 = x + dx;
							int y2 = y + dy;
							if (able[dir] == false) continue;

							double nearestStepNext = nearestStepNow;
							for (int ai2 = 0; ai2 < 4; ai2++) {
								if (ai2 == ai) continue;

								// TODO 友達の計算の仕方をもう少し賢くする。
								int index1 = ai2 * numt * numField * numField + (t + 0) * numField * numField + y * numField + x;
								int index2 = ai2 * numt * numField * numField + (t + 0) * numField * numField + y2 * numField + x2;
								int index3 = ai2 * numt * numField * numField + (t + 1) * numField * numField + y * numField + x;
								int index4 = ai2 * numt * numField * numField + (t + 1) * numField * numField + y2 * numField + x2;

								double[] stepMap;
								if (teamNumber[ai] == teamNumber[ai2]) {
									stepMap = stepMaps_stay;
								} else {
									stepMap = stepMaps_move;
								}

								double stepNext = stepMap[index4];
								double stepCross1 = stepMap[index2];
								double stepCross2 = stepMap[index3];

								if (stepNext != numt) {
									if (stepNext < nearestStepNext) {
										nearestStepNext = stepNext;
									}
								} else {
									if (stepCross1 != numt && stepCross2 != numt) {
										if (stepCross2 < nearestStepNext) {
											nearestStepNext = Math.min(stepCross1, stepCross2);
										}
									}
								}
							}
							int index2 = ai * numt * numField * numField + (t + 1) * numField * numField + y2 * numField + x2;
							if (nearestStepNext > hitNearestStep[index2]) {
								hitNearestStep[index2] = nearestStepNext;
							}
						}
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// 盤面系列からスコアを計算する。
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		double[][] scores = new double[4][2];

		// 縮小断面積
		if (true) {
			for (int ai = 0; ai < 4; ai++) {
				Pack packInit = packsNA[0];
				AgentEEE agentInit = packInit.sh.getAgent(ai + 10);
				if (agentInit == null) continue;

				int t = numt - 1;
				double[] gain = new double[numt + 1];
				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						int index = ai * numt * numField * numField + t * numField * numField + y * numField + x;
						int step = (int) hitNearestStep[index];
						if (step < 0) continue;
						gain[step]++;
					}
				}

				for (int s = 0; s < numt; s++) {
					gain[s] = gain[s] - gainOffset;
					if (gain[s] < 0) gain[s] = 0;
				}

				double total = 0;
				for (int s = 0; s < numt + 1; s++) {
					total += Math.pow(rateLevel, s - numt) * gain[s];
				}

				scores[ai][0] += total;
				scores[ai][1] += 1;
			}
		}

		// 縮小断面積、全時間考慮
		if (false) {
			// double rate = timeDecayRate;
			double rate = 1;

			for (int ai = 0; ai < 4; ai++) {
				Pack packInit = packsNA[0];
				AgentEEE agentInit = packInit.sh.getAgent(ai + 10);
				if (agentInit == null) continue;

				double totaltotal = 0;
				double weighttotal = 0;
				for (int t : new int[] { 5, numt - 1 }) {
					// for (int t : new int[] { numt - 1 }) {
					// for (int t : new int[] { 1, 6, numt - 1 }) {
					// for (int t = 1; t < numt; t++) {

					double[] gain = new double[numt + 1];
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							int index = ai * numt * numField * numField + t * numField * numField + y * numField + x;
							int step = (int) hitNearestStep[index];
							if (step < 0) continue;
							gain[step]++;
						}
					}

					for (int s = 0; s < numt; s++) {
						gain[s] = gain[s] - gainOffset;
						if (gain[s] < 0) gain[s] = 0;
					}

					double total = 0;
					for (int s = 0; s < numt + 1; s++) {
						total += Math.pow(rateLevel, s - numt) * gain[s];
					}

					double weight = Math.pow(rate, t);
					totaltotal += total * weight;
					weighttotal += weight;
				}

				scores[ai][0] += totaltotal / weighttotal;
				scores[ai][1] += 1;
			}
		}

		return scores;
	}
}
