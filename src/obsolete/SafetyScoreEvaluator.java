package obsolete;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.ibm.trl.BBM.mains.BBMUtility;
import com.ibm.trl.BBM.mains.ForwardModel;
import com.ibm.trl.BBM.mains.GlobalParameter;
import com.ibm.trl.BBM.mains.StatusHolder;
import com.ibm.trl.BBM.mains.BBMUtility.SurroundedInformation;
import com.ibm.trl.BBM.mains.ForwardModel.Pack;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;
import com.ibm.trl.BBM.mains.StatusHolder.EEE;

public class SafetyScoreEvaluator {

	static final Random rand = new Random();
	static final int numThread = GlobalParameter.numThread;
	static final int numField = GlobalParameter.numField;

	static final ForwardModel fm = new ForwardModel();
	static final double decayRate = 1;
	static final int numt = 12;
	static final int numTry = 5;
	static final double[] weights = new double[numt];
	static List<Task_ComputeSafetyScore> tasks = new ArrayList<Task_ComputeSafetyScore>();

	static {
		for (int t = 0; t < numt; t++) {
			weights[t] = Math.pow(decayRate, t);
		}

		for (int i = 0; i < numThread; i++) {
			Task_ComputeSafetyScore task = new Task_ComputeSafetyScore();
			task.start();
			tasks.add(task);
		}
	}

	static public class Task_ComputeSafetyScore extends Thread {

		int me = -1;
		Pack pack = null;
		double[][][] result = new double[2][4][6];
		int tryCounter = 0;

		@Override
		public void run() {
			try {
				Pack packLast = null;
				while (true) {
					Pack packLocal;
					int meLocal;
					synchronized (this) {
						packLocal = this.pack;
						meLocal = this.me;
					}
					if (packLocal == null) {
						continue;
					}

					if (packLocal != packLast) {
						packLast = packLocal;
						synchronized (result) {
							for (int i = 0; i < 2; i++) {
								for (int ai = 0; ai < 4; ai++) {
									for (int act = 0; act < 6; act++) {
										result[i][ai][act] = 0;
									}
								}
							}
							tryCounter = 0;
						}
					}

					double[][][] temp = SafetyScoreEvaluator.evaluateSafetyScore(numTry, packLocal, meLocal);

					synchronized (result) {
						for (int i = 0; i < 2; i++) {
							for (int ai = 0; ai < 4; ai++) {
								for (int act = 0; act < 6; act++) {
									result[i][ai][act] += temp[i][ai][act];
								}
							}
						}
						tryCounter += 1;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	public static double[][][] evaluateSafetyScore(int numTry, Pack pack, int me) throws Exception {

		Pack packNow = pack;

		// TODO �c���̕]��
		{
			Pack packNext = pack;

			boolean[][] bombExistNext = new boolean[numField][numField];
			for (EEE bbb : packNext.sh.getBombEntry()) {
				bombExistNext[bbb.x][bbb.y] = true;
			}

			for (AgentEEE aaa : packNext.sh.getAgentEntry()) {
				int ai = aaa.agentID - 10;
				int scoreTateana = BBMUtility.GGG_Tateana(packNext.board, bombExistNext, aaa.x, aaa.y, packNext.abs[ai].kick, packNext.abs[me - 10].strength);
			}
		}

		//////////////////////////////////////////////////////////////////
		// �S�A�N�V������SurvivableScore���v�Z����B
		//////////////////////////////////////////////////////////////////
		double[][] points = new double[4][6];
		double[][] pointsTotal = new double[4][6];
		for (int targetFirstAction = 0; targetFirstAction < 6; targetFirstAction++) {
			for (int tryIndex = 0; tryIndex < numTry; tryIndex++) {
				Pack packNext = packNow;
				for (int t = 0; t < numt; t++) {

					AgentEEE agentsNow[] = new AgentEEE[4];
					for (AgentEEE aaa : packNext.sh.getAgentEntry()) {
						agentsNow[aaa.agentID - 10] = aaa;
					}

					boolean[][] bombExistNow = new boolean[numField][numField];
					for (EEE bbb : packNext.sh.getBombEntry()) {
						bombExistNow[bbb.x][bbb.y] = true;
					}

					// ��肤��A�N�V������񋓂��āA��������B
					int[] actions = new int[4];
					for (int ai = 0; ai < 4; ai++) {
						if (t == 0 && ai == me - 10) {
							actions[ai] = targetFirstAction;
						} else {
							actions[ai] = rand.nextInt(6);
						}
					}

					packNext = fm.Step(packNext.board, packNext.abs, packNext.sh, actions);

					double weight = weights[t];

					AgentEEE agentsNext[] = new AgentEEE[4];
					for (AgentEEE aaa : packNext.sh.getAgentEntry()) {
						agentsNext[aaa.agentID - 10] = aaa;
					}

					boolean[][] bombExistNext = new boolean[numField][numField];
					for (EEE bbb : packNext.sh.getBombEntry()) {
						bombExistNext[bbb.x][bbb.y] = true;
					}

					for (int ai = 0; ai < 4; ai++) {
						if (packNow.abs[ai].isAlive == false) continue;

						double ppp = 1;
						if (packNext.abs[ai].isAlive == false) {
							ppp = 0;
						} else {
							AgentEEE aaa = agentsNext[ai];

							SurroundedInformation si = BBMUtility.numSurrounded_Rich(packNext.board, bombExistNext, aaa.x, aaa.y);
							if (si.numWall + si.numBombFixed + si.numBombFixedByAgent + si.numBombKickable + si.numAgent == 4 && si.numWall == 3) {
								if (si.numBombFixed == 1) {
									ppp = -16;
								} else if (si.numBombFixedByAgent == 1) {
									ppp = -12;
								} else if (si.numBombKickable == 1) {
									ppp = -8;
								} else if (si.numAgent == 1) {
									ppp = -4;
								}
							}
						}

						pointsTotal[ai][targetFirstAction] += weight;
						points[ai][targetFirstAction] += weight * ppp;
					}
				}
			}
		}

		double[][][] ret = new double[2][][];
		ret[0] = points;
		ret[1] = pointsTotal;
		return ret;
	}

	static public void set(Pack pack, int me) throws Exception {
		for (Task_ComputeSafetyScore task : tasks) {
			synchronized (task) {
				task.me = me;
				task.pack = pack;
			}
		}
	}

	static public double[][] getLatestSafetyScore() {
		double[][][] temp = new double[2][4][6];
		for (Task_ComputeSafetyScore task : tasks) {
			synchronized (task.result) {
				for (int i = 0; i < 2; i++) {
					for (int ai = 0; ai < 4; ai++) {
						for (int act = 0; act < 6; act++) {
							temp[i][ai][act] += task.result[i][ai][act];
						}
					}
				}
			}
		}

		double[][] safetyScore = new double[4][6];
		for (int ai = 0; ai < 4; ai++) {
			for (int act = 0; act < 6; act++) {
				safetyScore[ai][act] = temp[0][ai][act] / temp[1][ai][act];
			}
		}

		return safetyScore;
	}

	static public double[][] computeSafetyScore(Pack pack, int me) throws Exception {
		double[][][] temp = SafetyScoreEvaluator.evaluateSafetyScore(500, pack, me);

		double[][] safetyScore = new double[4][6];
		for (int ai = 0; ai < 4; ai++) {
			for (int act = 0; act < 6; act++) {
				safetyScore[ai][act] = temp[0][ai][act] / temp[1][ai][act];
			}
		}

		return safetyScore;
	}

	static public int getTryCounter() {
		int tryCounter = 0;
		for (Task_ComputeSafetyScore task : tasks) {
			synchronized (task.result) {
				tryCounter += task.tryCounter;
			}
		}
		return tryCounter;
	}

}
