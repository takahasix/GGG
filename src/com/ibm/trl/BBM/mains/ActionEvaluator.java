package com.ibm.trl.BBM.mains;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.ibm.trl.BBM.mains.Agent.Ability;
import com.ibm.trl.BBM.mains.Agent.Node;
import com.ibm.trl.BBM.mains.ForwardModel.Pack;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;
import com.ibm.trl.BBM.mains.StatusHolder.EEE;

import ibm.ANACONDA.Core.MatrixUtility;
import ibm.ANACONDA.Core.MyMatrix;

public class ActionEvaluator {
	int numField;

	Epoch currentEpoch = new Epoch();
	List<Epoch> epochs = new ArrayList<Epoch>();

	static public class Epoch implements Serializable {
		private static final long serialVersionUID = 3199802952255862718L;
		List<Pack> packList = new ArrayList<Pack>();
		int me;
		double reward;
	}

	public ActionEvaluator(int numField) {
		this.numField = numField;
	}

	/**
	 * �A�N�V���������肷��B
	 */
	static Random rand = new Random();

	double thresholdMoveToItem = 0;
	double thresholdMoveToWood = 0;
	double thresholdBombForWood = 0;

	public int ComputeOptimalAction(int me, MyMatrix board, Node[][] bombMap, Ability abs[]) throws Exception {

		int myAgentIndex = me - 10;

		//////////////////////////////////////////////////////////////////
		// ������Ԃ����B
		//////////////////////////////////////////////////////////////////
		MyMatrix boardNow = new MyMatrix(board);

		Ability[] absNow = new Ability[4];
		for (int i = 0; i < 4; i++) {
			absNow[i] = new Ability(abs[i]);
		}

		StatusHolder shNow = new StatusHolder(numField);
		{
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					int type = (int) board.data[x][y];
					if (Constant.isAgent(type)) {
						shNow.setAgent(x, y, type);
					}
				}
			}

			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					Node node = bombMap[x][y];
					if (node == null) continue;
					if (node.type == Constant.Bomb) {
						shNow.setBomb(x, y, node.owner, node.lifeBomb, node.moveDirection, node.power);
					} else if (node.type == Constant.Flames) {
						shNow.setFlameCenter(x, y, node.lifeFlameCenter, node.power);
					}
				}
			}
		}

		AgentEEE[] agentsNow = new AgentEEE[4];
		for (AgentEEE aaa : shNow.getAgentEntry()) {
			agentsNow[aaa.agentID - 10] = aaa;
		}

		Pack packNow = new Pack(boardNow, absNow, shNow);

		//////////////////////////////////////////////////////////////////
		// �S�A�N�V������SurvivableScore���v�Z����B
		//////////////////////////////////////////////////////////////////

		double[] safetyScore = new double[6];
		double[] safetyDefScore = new double[6];
		double[][] safetyScoreAll = new double[4][6];
		{
			ForwardModel fm = new ForwardModel(numField);
			double decayRate = 0.99;
			int numt = 10;
			int numTry = 500;

			for (int targetAction = 0; targetAction < 6; targetAction++) {
				double[] points = new double[4];
				double[] pointsTotal = new double[4];
				for (int tryIndex = 0; tryIndex < numTry; tryIndex++) {
					Pack packNext = packNow;
					for (int t = 0; t < numt; t++) {
						int[] actions = { rand.nextInt(6), rand.nextInt(6), rand.nextInt(6), rand.nextInt(6) };
						if (t == 0) {
							actions[myAgentIndex] = targetAction;
						}

						// TODO Agent, Bomb, Flame�̏��Ԃɕ���ł���Ƃ�������m���Ď~�߂�B
						if (false) {
							for (int dir = 1; dir <= 4; dir++) {
								for (int x = 0; x < numField; x++) {
									for (int y = 0; y < numField; y++) {

										int x2 = x;
										int y2 = y;
										int x3 = x;
										int y3 = y;

										if (dir == 1) {
											x2 -= 1;
											x3 -= 2;
										} else if (dir == 2) {
											x2 += 1;
											x3 += 2;
										} else if (dir == 3) {
											y2 -= 1;
											y3 -= 2;
										} else if (dir == 4) {
											y2 += 1;
											y3 += 2;
										}
										if (x3 < 0 || x3 >= numField || y3 < 0 || y3 >= numField) continue;

										int type = (int) packNow.board.data[x][y];
										int type2 = (int) packNow.board.data[x2][y2];
										int type3 = (int) packNow.board.data[x3][y3];

										if (Constant.isAgent(type) && type2 == Constant.Bomb && type3 == Constant.Flames) {
											if (actions[type - 10] == dir) {
												System.out.println("���e��Flames�ɉ����������Ƃ��Ă��邺��B");
											}
										}
									}
								}
							}
						}

						packNext = fm.Step(packNext.board, packNext.abs, packNext.sh, actions);

						double weight = Math.pow(decayRate, t);

						AgentEEE agentsNext[] = new AgentEEE[4];
						for (AgentEEE aaa : packNext.sh.getAgentEntry()) {
							agentsNext[aaa.agentID - 10] = aaa;
						}

						for (int ai = 0; ai < 4; ai++) {
							if (packNow.abs[ai].isAlive == false) continue;

							double good = weight;
							if (packNext.abs[ai].isAlive == false) {
								good = 0;
							} else {
								AgentEEE aaa = agentsNext[ai];
								if (BBMUtility.isSurrounded(numField, packNext.board, aaa.x, aaa.y)) {
									good = -weight;
								}
							}

							pointsTotal[ai] += weight;
							points[ai] += good;
						}
					}
				}

				double[] scores = new double[4];
				for (int ai = 0; ai < 4; ai++) {
					scores[ai] = points[ai] / pointsTotal[ai];
				}

				double scoreMe = 0;
				double scoreSumOther = 0;
				double numOther = 0;
				for (int ai = 0; ai < 4; ai++) {
					if (abs[ai].isAlive == false) continue;
					if (ai == myAgentIndex) {
						scoreMe = scores[ai];
					} else {
						scoreSumOther += scores[ai];
						numOther++;
					}
				}
				double scoreOther = scoreSumOther / numOther;

				double scoreDef = scoreMe - scoreOther;

				safetyScore[targetAction] = scoreMe;
				safetyDefScore[targetAction] = scoreDef;
				for (int ai = 0; ai < 4; ai++) {
					safetyScoreAll[ai][targetAction] = scores[ai];
				}
			}

			for (int i = 0; i < 6; i++) {
				System.out.println("action=" + i + ", safetyScore=" + safetyScore[i]);
			}
			// for (int i = 0; i < 6; i++) {
			// System.out.println("action=" + i + ", safetyDefScore=" + safetyDefScore[i]);
			// }
		}

		// �������v�Z����B
		{
			AgentEEE agentMe = agentsNow[myAgentIndex];
			MyMatrix dis = computeOptimalDistance(packNow.board, agentMe.x, agentMe.y);
			Ability ab = abs[myAgentIndex];

			MyMatrix thresholdMoveToItem = new MyMatrix(3, 10, 0.65);
			MyMatrix thresholdMoveToWoodBrake = new MyMatrix(4, 10, 0.85);
			MyMatrix thresholdBombToWoodBrake = new MyMatrix(4, 1, 0.75);
			MyMatrix thresholdAttack = new MyMatrix(2, 1, 0.6);
			thresholdAttack.data[1][0] = 0.6;

			double scoreBest = 0;
			int actionBest = -1;
			String reasonBest = "";

			// �ړ��n�ł�����T���B
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					int type = (int) packNow.board.data[x][y];
					int d = (int) dis.data[x][y];
					if (d > 1000) continue;

					double score = -Double.MAX_VALUE;
					int dir = -1;
					String reason = "";
					if (type == Constant.ExtraBomb) {
						int d2 = d - 1;
						if (d2 > 9) d2 = 9;
						double threshold = thresholdMoveToItem.data[0][d2];
						dir = ComputeFirstDirection(dis, x, y);
						score = safetyScore[dir] - threshold;
						reason = "ExtraBomb";
					} else if (type == Constant.IncrRange) {
						int d2 = d - 1;
						if (d2 > 9) d2 = 9;
						double threshold = thresholdMoveToItem.data[1][d2];
						dir = ComputeFirstDirection(dis, x, y);
						score = safetyScore[dir] - threshold;
						reason = "IncrRange";
					} else if (type == Constant.Kick) {
						int d2 = d - 1;
						if (d2 > 9) d2 = 9;
						double threshold = thresholdMoveToItem.data[2][d2];
						dir = ComputeFirstDirection(dis, x, y);
						score = safetyScore[dir] - threshold;
						reason = "Kick";
					} else {
						int num = BBMUtility.numWoodBrakable(numField, boardNow, x, y, ab.strength);
						if (num > 0) {
							int d2 = d;
							if (d2 > 9) d2 = 9;
							double threshold = thresholdMoveToWoodBrake.data[num - 1][d2];
							dir = ComputeFirstDirection(dis, x, y);
							score = safetyScore[dir] - threshold;
							reason = "move to Wood Brake";
						}
					}

					if (score > scoreBest) {
						scoreBest = score;
						actionBest = dir;
						reasonBest = reason;
					}
				}
			}

			// �ؔj��̂��߂̔��e�ݒu�ł�����T���B
			if (ab.numBombHold > 0) {
				int num = BBMUtility.numWoodBrakable(numField, boardNow, agentMe.x, agentMe.y, ab.strength);
				if (num > 0) {
					double threshold = thresholdBombToWoodBrake.data[num - 1][0];
					double score = safetyScore[5] - threshold;
					int action = 5;
					if (score > scoreBest) {
						scoreBest = score;
						actionBest = action;
						reasonBest = "bomb for Wood Brake";
					}
				}
			}

			// �G���E�����߂̔��e�ݒu�ł������c�T���B
			if (ab.numBombHold > 0) {
				for (int ai = 0; ai < 4; ai++) {
					if (ai == myAgentIndex) continue;

					double thresholdMe = thresholdAttack.data[0][0];
					double thresholdEnemy = thresholdAttack.data[1][0];

					// �����̃A�N�V�����Ŏ��ɂ����ɐ���̂��A�������Ȃ��Ă����ɂ����Ȃ̂����ׂ�B
					double best = -Double.MAX_VALUE;
					double bad = Double.MAX_VALUE;
					int actBad = -1;
					for (int act = 0; act < 6; act++) {
						if (safetyScoreAll[ai][act] > best) {
							best = safetyScoreAll[ai][act];
						}
						if (safetyScoreAll[ai][act] < bad) {
							bad = safetyScoreAll[ai][act];
							actBad = act;
						}
					}
					if (best - bad < 0.1) continue;

					// �G��臒l�������A�N�V�����ŁA���������S�ȃ��c��T���B
					// for (int act = 0; act < 6; act++) {
					{
						// int act = actBad;
						// if (safetyScoreAll[ai][act] > thresholdEnemy) continue;
						double score = safetyScore[actBad] - thresholdMe;
						if (score > scoreBest) {
							scoreBest = score;
							actionBest = actBad;
							reasonBest = "attack";
						}
					}
				}
			}

			// �S�Ă̍s����臒l�𒴂��Ă��Ȃ��ꍇ�́A�댯�ȏ�ԂȂ̂ŁA�����Ƃ����S�ɂȂ�s����I�ԁB
			if (actionBest == -1) {
				for (int act = 0; act < 6; act++) {
					double score = safetyScore[act];
					if (score > scoreBest) {
						scoreBest = score;
						actionBest = act;
						reasonBest = "most safety";
					}
				}
			}

			String actionStr = "";
			if (actionBest == 0) {
				actionStr = "�E";
			} else if (actionBest == 1) {
				actionStr = "��";
			} else if (actionBest == 2) {
				actionStr = "��";
			} else if (actionBest == 3) {
				actionStr = "��";
			} else if (actionBest == 4) {
				actionStr = "��";
			} else if (actionBest == 5) {
				actionStr = "��";

			}
			System.out.println(reasonBest + ", " + actionBest + ", " + actionStr);

			return actionBest;
		}

	}

	/**
	 * ���X�e�b�v�Ă΂�āA�Ֆʂ̏�Ԃ�����ʂɗ��Ƃ��āA�G�|�b�N�n��Ƃ��ċL�^����B
	 */
	public void Sample(MyMatrix board, Node[][] bombMap, Ability abs[]) throws Exception {

		// TODO ���������ɉe���������ȓ����ʂ����낢�뎎���Ă݂�B
		test();

		//////////////////////////////////////////////////////////////////
		// ������Ԃ����B
		//////////////////////////////////////////////////////////////////

		MyMatrix boardNow = new MyMatrix(board);

		Ability[] absNow = new Ability[4];
		for (int i = 0; i < 4; i++) {
			absNow[i] = new Ability(abs[i]);
		}

		StatusHolder shNow = new StatusHolder(numField);
		{
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					int type = (int) board.data[x][y];
					if (Constant.isAgent(type)) {
						shNow.setAgent(x, y, type);
					}
				}
			}

			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					Node node = bombMap[x][y];
					if (node == null) continue;
					if (node.type == Constant.Bomb) {
						shNow.setBomb(x, y, node.owner, node.lifeBomb, node.moveDirection, node.power);
					} else if (node.type == Constant.Flames) {
						shNow.setFlameCenter(x, y, node.lifeFlameCenter, node.power);
					}
				}
			}
		}

		Pack pack = new Pack(boardNow, absNow, shNow);

		currentEpoch.packList.add(pack);
	}

	/**
	 * �G�|�b�N���I�����āA��V���m�肵���Ƃ��ɌĂяo�����B
	 */
	public void FinishOneEpoch(int me, double reword) throws Exception {
		currentEpoch.me = me;
		currentEpoch.reward = reword;
		epochs.add(currentEpoch);
		currentEpoch = new Epoch();

		// �G�|�b�N��100�ȏソ�܂�����A�t�@�C���ɕۑ�����B
		if (epochs.size() > 100) {

			String filename;
			for (int i = 0;; i++) {
				String filename2 = String.format("data/epochs_%05d.dat", i);
				if (new File(filename2).exists() == false) {
					filename = filename2;
					break;
				}
			}
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(filename)));
			oos.writeObject(epochs);
			oos.flush();
			oos.close();

			epochs.clear();
		}
	}

	public void test() throws Exception {
		List<Epoch> epochs = new ArrayList<Epoch>();
		File dir = new File("data");
		for (File file : dir.listFiles()) {
			if (file.getName().startsWith("epochs_") == false) continue;
			System.out.println("file reading, " + file.getName());
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			List<Epoch> temp = (List<Epoch>) ois.readObject();
			epochs.addAll(temp);
		}
		test2(epochs);
	}

	public MyMatrix computeOptimalDistance(MyMatrix board, int sx, int sy) throws Exception {

		boolean[][] blocked = new boolean[numField][numField];
		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				int type = (int) board.data[x][y];
				if (type == Constant.Rigid || type == Constant.Wood || type == Constant.Flames || type == Constant.Bomb) {
					blocked[x][y] = true;
				}
			}
		}

		int[][] currentPositions = new int[122][2];
		int[][] nextPositions = new int[122][2];

		double[][] dis = new double[numField][numField];
		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				dis[x][y] = Double.MAX_VALUE;
			}
		}
		dis[sx][sy] = 0;

		currentPositions[0][0] = sx;
		currentPositions[0][1] = sy;
		currentPositions[1][0] = -1;

		while (true) {
			int counter = 0;
			for (int[] targetPos : currentPositions) {
				if (targetPos[0] == -1) break;
				int x = targetPos[0];
				int y = targetPos[1];
				if (x > 0) {
					int x2 = x - 1;
					int y2 = y;
					if (blocked[x2][y2] == false) {
						double temp1 = dis[x][y] + 1;
						double temp2 = dis[x2][y2];
						if (temp1 < temp2) {
							dis[x2][y2] = temp1;
							nextPositions[counter][0] = x2;
							nextPositions[counter][1] = y2;
							counter++;
						}
					}
				}
				if (x < numField - 1) {
					int x2 = x + 1;
					int y2 = y;
					if (blocked[x2][y2] == false) {
						double temp1 = dis[x][y] + 1;
						double temp2 = dis[x2][y2];
						if (temp1 < temp2) {
							dis[x2][y2] = temp1;
							nextPositions[counter][0] = x2;
							nextPositions[counter][1] = y2;
							counter++;
						}
					}
				}
				if (y > 0) {
					int x2 = x;
					int y2 = y - 1;
					if (blocked[x2][y2] == false) {
						double temp1 = dis[x][y] + 1;
						double temp2 = dis[x2][y2];
						if (temp1 < temp2) {
							dis[x2][y2] = temp1;
							nextPositions[counter][0] = x2;
							nextPositions[counter][1] = y2;
							counter++;
						}
					}
				}
				if (y < numField - 1) {
					int x2 = x;
					int y2 = y + 1;
					if (blocked[x2][y2] == false) {
						double temp1 = dis[x][y] + 1;
						double temp2 = dis[x2][y2];
						if (temp1 < temp2) {
							dis[x2][y2] = temp1;
							nextPositions[counter][0] = x2;
							nextPositions[counter][1] = y2;
							counter++;
						}
					}
				}
			}

			nextPositions[counter][0] = -1;
			counter++;

			if (counter == 1) break;

			int[][] temp = currentPositions;
			currentPositions = nextPositions;
			nextPositions = temp;
		}

		return new MyMatrix(dis);
	}
	
	public int ComputeFirstDirection(MyMatrix dis, int xNow, int yNow) {
		int disNow = (int) dis.data[xNow][yNow];

		int dir = 0;
		while (true) {
			if (disNow == 0) {
				break;
			}

			if (xNow > 0) {
				int xNew = xNow - 1;
				int yNew = yNow;
				int temp = (int) dis.data[xNew][yNew];
				if (temp == disNow - 1) {
					disNow = temp;
					xNow = xNew;
					yNow = yNew;
					dir = 2;
					continue;
				}
			}

			if (xNow < numField - 1) {
				int xNew = xNow + 1;
				int yNew = yNow;
				int temp = (int) dis.data[xNew][yNew];
				if (temp == disNow - 1) {
					disNow = temp;
					xNow = xNew;
					yNow = yNew;
					dir = 1;
					continue;
				}
			}

			if (yNow > 0) {
				int xNew = xNow;
				int yNew = yNow - 1;
				int temp = (int) dis.data[xNew][yNew];
				if (temp == disNow - 1) {
					disNow = temp;
					xNow = xNew;
					yNow = yNew;
					dir = 4;
					continue;
				}
			}

			if (yNow < numField - 1) {
				int xNew = xNow;
				int yNew = yNow + 1;
				int temp = (int) dis.data[xNew][yNew];
				if (temp == disNow - 1) {
					disNow = temp;
					xNow = xNew;
					yNow = yNew;
					dir = 3;
					continue;
				}
			}
		}

		return dir;
	}

	public void test2(List<Epoch> epochs) throws Exception {

		System.out.println("start");

		List<MyMatrix> Xs = new ArrayList<MyMatrix>();
		List<MyMatrix> Ys = new ArrayList<MyMatrix>();

		for (Epoch epoch : epochs) {

			int numPack = epoch.packList.size();
			System.out.println(numPack);

			if (false) {
				Pack packLast = epoch.packList.get(epoch.packList.size() - 1);
				Ability ab = packLast.abs[3];
				System.out.println("Ability, " + ab.numMaxBomb + ", " + ab.strength + ", " + ab.kick + "," + epoch.reward);
			}

			////////////////////////////////////////////////////////////////
			// �ړ������v�Z�̃e�X�g
			////////////////////////////////////////////////////////////////
			if (false) {
				for (int i = 0; i < numPack; i++) {
					Pack pack = epoch.packList.get(i);
					for (EEE eee : pack.sh.getAgentEntry()) {
						computeOptimalDistance(pack.board, eee.x, eee.y);
					}
				}
			}

			////////////////////////////////////////////////////////////////
			// Item�Ƃ̋����Ƃ��AAbility�n�̓����ʂ����B
			////////////////////////////////////////////////////////////////

			if (true) {

				double decayRate = 0.99;

				for (int i = 0; i < numPack; i++) {
					Pack pack = epoch.packList.get(i);

					Ability ab = pack.abs[epoch.me - 10];

					// �����̃G�[�W�F���g���E���B
					AgentEEE agent = null;
					for (AgentEEE eee : pack.sh.getAgentEntry()) {
						if (eee.agentID == epoch.me) {
							agent = eee;
						}
					}

					MyMatrix X = new MyMatrix(78, 1);
					int base = 0;

					// �������v�Z����B
					MyMatrix dis = computeOptimalDistance(pack.board, agent.x, agent.y);

					// ���e�󂹂�ꏊ�ōŒZ�����̏ꏊ��T���B
					{
						double mindis = Double.MAX_VALUE;
						for (int x = 0; x < numField; x++) {
							for (int y = 0; y < numField; y++) {
								if (dis.data[x][y] < mindis) {
									if (BBMUtility.numWoodBrakable(numField, pack.board, x, y, 2) > 0) {
										mindis = dis.data[x][y];
									}
								}
							}
						}

						if (ab.strength == 2) {
							int temp = (int) mindis;
							if (temp > 10) temp = 10;
							X.data[base + temp][0] = 1;
						}
						base += 11;
					}

					// �A�C�e���擾�\�̏ꏊ��T���B
					if (true) {
						for (int target : new int[] { Constant.ExtraBomb, Constant.IncrRange, Constant.Kick }) {
							double mindis = Double.MAX_VALUE;
							for (int x = 0; x < numField; x++) {
								for (int y = 0; y < numField; y++) {
									int type = (int) pack.board.data[x][y];
									if (type == target) {
										if (dis.data[x][y] < mindis) {
											mindis = dis.data[x][y];
										}
									}
								}
							}

							if (true) {
								// if (ab.strength == 2) {
								// if (ab.numMaxBomb == 1) {
								int temp = (int) mindis;
								if (temp > 10) temp = 10;
								X.data[base + temp][0] = 1;
							}
							base += 11;
						}
					}

					// Ability������x�N�g��������B
					{
						int temp = ab.numMaxBomb - 1;
						if (temp > 10) temp = 10;
						X.data[base + temp][0] = 1;
						base += 11;
					}
					{
						int temp = ab.numBombHold;
						if (temp > 10) temp = 10;
						X.data[base + temp][0] = 1;
						base += 11;
					}
					{
						int temp = ab.strength - 2;
						if (temp > 10) temp = 10;
						X.data[base + temp][0] = 1;
						base += 11;
					}
					{
						if (ab.kick) {
							X.data[base][0] = 1;
						}
						base += 1;
					}

					/////////////////////////
					// ���̏�Base��78����

					MyMatrix y = new MyMatrix(new double[][] { { Math.pow(decayRate, numPack - i - 1) } });

					Xs.add(X);
					Ys.add(y);
				}
			}

			////////////////////////////////////////////////////////////////
			// SurvivableRate�̌v�Z
			////////////////////////////////////////////////////////////////
			if (false) {
				// �Ō�Ɉ�l�����c���Ă���P�[�X������Ώۂɂ���B�����c���Ă��Ď��Ԑ؂�̃P�[�X�͍l�����Ȃ��B
				if (numPack < 800) {

					Random rand = new Random();
					double decayRate = 0.95;
					int numFrame = 12;
					int numTry = 10000;
					ForwardModel fm = new ForwardModel(numField);
					int agentIndex = epoch.me - 10;
					// for (int i = numPack - 3; i < numPack; i++) {
					for (int i = 0; i < numPack; i++) {
						Pack pack = epoch.packList.get(i);
						Pack packNow = pack;

						double[] survivablePoint = new double[4];
						double[] survivablePointTotal = new double[4];
						for (int tryIndex = 0; tryIndex < numTry; tryIndex++) {
							for (int frame = 0; frame < numFrame; frame++) {
								int[] actions = { rand.nextInt(6), rand.nextInt(6), rand.nextInt(6), rand.nextInt(6) };
								// int[] actions = { rand.nextInt(5), rand.nextInt(5), rand.nextInt(5), rand.nextInt(5) };
								packNow = fm.Step(packNow.board, packNow.abs, packNow.sh, actions);

								double point = Math.pow(decayRate, frame);
								for (int ai = 0; ai < 4; ai++) {
									if (pack.abs[ai].isAlive) {
										survivablePointTotal[ai] += point;
										if (packNow.abs[ai].isAlive) {
											survivablePoint[ai] += point;
										}
									}
								}
							}
						}

						double[] survivableRate = new double[4];
						for (int ai = 0; ai < 4; ai++) {
							survivableRate[ai] = survivablePoint[ai] / survivablePointTotal[ai];
						}

						double me = survivableRate[agentIndex];
						double sum = 0;
						double num = 0;
						for (int ai = 0; ai < 4; ai++) {
							if (ai == agentIndex) continue;
							if (pack.abs[ai].isAlive) {
								sum += survivableRate[ai];
								num++;
							}
						}
						double ave = sum / num;

						int numAliveAgent = 0;
						for (int ai = 0; ai < 4; ai++) {
							if (pack.abs[ai].isAlive) {
								numAliveAgent++;
							}
						}

						String line = String.format("me, %f, ave, %f, numAliveAgent, %d, rewrd, %f", me, ave, numAliveAgent, epoch.reward);
						System.out.println(line);
					}
					System.out.println();
				}
			}
		}

		{
			int numt = Xs.size();
			int numd = Xs.get(0).numt;

			MyMatrix X = new MyMatrix(numt, numd);
			MyMatrix Y = new MyMatrix(numt, 1);

			for (int t = 0; t < numt; t++) {
				MyMatrix x = Xs.get(t);
				MyMatrix y = Ys.get(t);
				for (int d = 0; d < numd; d++) {
					X.data[t][d] = x.data[d][0];
				}
				Y.data[t][0] = y.data[0][0];
			}

			// X��Y�̑��ւ��v�Z����B
			MyMatrix A = MatrixUtility.ConnectColumn(Y, X);
			A = MatrixUtility.Whitening(A);
			MyMatrix Z = A.transpose().times(A);
			MyMatrix S = MatrixUtility.ComputeCorrelationFromVariance(Z);
			MatrixUtility.OutputMatrix(S);
		}

		System.out.println("end");
	}
}
