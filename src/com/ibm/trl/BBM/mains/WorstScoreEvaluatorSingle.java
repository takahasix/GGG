package com.ibm.trl.BBM.mains;

import java.util.Random;

import com.ibm.trl.BBM.mains.ForwardModel.Pack;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;

import ibm.ANACONDA.Core.MyMatrix;

public class WorstScoreEvaluatorSingle {

	static final Random rand = new Random();
	static final int numThread = GlobalParameter.numThread;
	static final int numField = GlobalParameter.numField;
	static final ForwardModel fm = new ForwardModel();
	static final int numt = 15;

	// �G�G�[�W�F���g���������đ����Ă����Ƃ��̑������B�傫������ƍň��P�[�X���ς���ɋ߂��Ȃ�B
	double divideRate_near = 0.99;
	double divideRate_far = 1.0;
	double decayRate = 0.9;
	int numtNear = 100;

	static class FootPrint {
		double score = 0;
		int x_pre = -1;
		int y_pre = -1;
	}

	public double Do(int me, int firstAction, Pack packNow, Pack packNow_nagent, Pack packNext_onlyme, Pack packNext_nagent) throws Exception {

		if (firstAction == 5) firstAction = 0;

		// TODO �G���[�`�F�b�N
		if (true) {
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					int type = (int) packNow_nagent.board.data[x][y];
					if (Constant.isAgent(type)) throw new Exception();
				}
			}
			if (packNow_nagent.sh.getAgentEntry().size() > 0) throw new Exception();

			// for (int x = 0; x < numField; x++) {
			// for (int y = 0; y < numField; y++) {
			// int type = (int) pack2_na.board.data[x][y];
			// if (Constant.isAgent(type)) throw new Exception();
			// }
			// }
			// if (pack2_na.sh.getAgentEntry().size() > 0) throw new Exception();
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// numt�X�e�b�v��܂ŃV�~�����[�V��������B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		Pack[] packs_nagent = new Pack[numt];
		if (true) {
			packs_nagent[0] = packNow_nagent;
			packs_nagent[1] = packNext_nagent;
			Pack packNext = packNext_nagent;
			int[] actions = new int[4];
			for (int t = 2; t < numt; t++) {
				packNext = fm.Step(packNext.board, packNext.flameLife, packNext.abs, packNext.sh, actions);
				packs_nagent[t] = packNext;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �G�[�W�F���g�𓮂����āA���݊m�����v�Z����B�����B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		MyMatrix[][] agentWeight = new MyMatrix[numt][4];

		for (int t = 0; t < numt; t++) {
			for (int ai = 0; ai < 4; ai++) {
				agentWeight[t][ai] = new MyMatrix(numField, numField);
			}
		}

		for (AgentEEE aaa : packNow.sh.getAgentEntry()) {
			int ai = aaa.agentID - 10;
			if (ai == me - 10) continue;
			agentWeight[0][ai].data[aaa.x][aaa.y] = 1;
		}

		for (int ai = 0; ai < 4; ai++) {
			if (ai == me - 10) continue;
			for (int t = 0; t < numt - 1; t++) {
				Pack packNext = packs_nagent[t + 1];

				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						double weight = agentWeight[t][ai].data[x][y];
						if (weight == 0) continue;

						// �J�ڐ�̐��𐔂���B
						double count = 0;
						boolean[] able = new boolean[5];
						for (int[] vec : GlobalParameter.onehopList) {
							int dir = vec[0];
							int dx = vec[1];
							int dy = vec[2];
							int x2 = x + dx;
							int y2 = y + dy;
							if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) continue;
							int type = (int) packNext.board.data[x][y];
							if (Constant.isWall(type)) continue;
							if (dir != 0 && type == Constant.Bomb) continue;
							if (type == Constant.Flames) continue;
							count++;
							able[dir] = true;
						}

						// 1/count�ɕ������Ď��X�e�b�v�̑��݊m���ɑ������ށB
						for (int[] vec : GlobalParameter.onehopList) {
							int dir = vec[0];
							int dx = vec[1];
							int dy = vec[2];
							int x2 = x + dx;
							int y2 = y + dy;
							if (able[dir] == false) continue;
							if (t <= numtNear) {
								double weightNext = weight * divideRate_near;
								if (weightNext > agentWeight[t + 1][ai].data[x2][y2]) {
									agentWeight[t + 1][ai].data[x2][y2] = weightNext;
								}
							} else {
								double weightNext = weight / count * divideRate_far;
								if (weightNext > agentWeight[t + 1][ai].data[x2][y2]) {
									agentWeight[t + 1][ai].data[x2][y2] = weightNext;
								}
							}
						}
					}
				}

				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						if (agentWeight[t + 1][ai].data[x][y] < agentWeight[t][ai].data[x][y]) {
							agentWeight[t + 1][ai].data[x][y] = agentWeight[t][ai].data[x][y];
						}
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �S�s���̌o�H�̃X�R�A���v�Z���Ă݂�B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		MyMatrix[] reachProb = new MyMatrix[numt];
		MyMatrix[] aliveProb = new MyMatrix[numt];

		if (true) {
			for (int t = 0; t < numt; t++) {
				reachProb[t] = new MyMatrix(numField, numField);
				aliveProb[t] = new MyMatrix(numField, numField);
			}
			for (AgentEEE aaa : packNow.sh.getAgentEntry()) {
				if (aaa.agentID != me) continue;
				reachProb[0].data[aaa.x][aaa.y] = 1;
				aliveProb[0].data[aaa.x][aaa.y] = 1;
			}
			for (AgentEEE aaa : packNext_onlyme.sh.getAgentEntry()) {
				if (aaa.agentID != me) continue;
				reachProb[1].data[aaa.x][aaa.y] = 1;
				aliveProb[1].data[aaa.x][aaa.y] = 1;
			}

			for (int t = 1; t < numt - 1; t++) {
				Pack packNext = packs_nagent[t + 1];
				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						double count = 0;
						boolean[] able = new boolean[5];
						for (int[] vec : GlobalParameter.onehopList) {
							int dir = vec[0];
							int dx = vec[1];
							int dy = vec[2];
							int x2 = x + dx;
							int y2 = y + dy;
							if (t == 0 && dir != firstAction) continue;
							if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) continue;
							int type = (int) packNext.board.data[x2][y2];
							if (Constant.isWall(type)) continue;
							if (dir != 0 && type == Constant.Bomb) continue;
							if (type == Constant.Flames) continue;
							able[dir] = true;
							count++;
						}
						if (count == 0) continue;

						for (int[] vec : GlobalParameter.onehopList) {
							int dir = vec[0];
							int dx = vec[1];
							int dy = vec[2];
							int x2 = x + dx;
							int y2 = y + dy;
							if (able[dir] == false) continue;

							double nonEnemyProb = 1;
							for (int ai = 0; ai < 4; ai++) {
								if (ai == me - 10) continue;
								double temp = agentWeight[t + 1][ai].data[x2][y2];
								if (temp > 1) temp = 1;
								nonEnemyProb *= (1 - temp);
							}

							// ���[�`�m���̌v�Z
							if (true) {
								double probNow = reachProb[t].data[x][y];
								if (probNow > 0) {
									double probNext = probNow * nonEnemyProb;
									if (probNext > reachProb[t + 1].data[x2][y2]) {
										reachProb[t + 1].data[x2][y2] = probNext;
									}
								}
							}

							// �����m���̌v�Z
							if (true) {
								double probNow = aliveProb[t].data[x][y];
								double moveNextProb = probNow * nonEnemyProb / count;
								double stayProb = probNow / count - moveNextProb;
								aliveProb[t + 1].data[x][y] += stayProb;
								aliveProb[t + 1].data[x2][y2] += moveNextProb;
							}
						}
					}
				}
			}
		}

		// �����m��
		if (false) {
			double temp = aliveProb[numt - 1].normL1();
			return temp;
		}

		// ���B�Z��
		if (true) {
			double temp = reachProb[numt - 1].normL1();
			return temp;
		}

		// ���B�Z���̑O�X�e�b�v���a
		if (true) {
			double sum = 0;
			double total = 0;
			for (int t = 1; t < numt; t++) {
				double temp = Math.pow(decayRate, t - 1);
				double len = reachProb[t].normL1();
				sum += len * temp;
				total += temp;
			}
			double ave = sum / total;
			return ave;
		}

		return 0;
	}
}
