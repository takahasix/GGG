package obsolete;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.trl.BBM.mains.BBMUtility;
import com.ibm.trl.BBM.mains.Constant;
import com.ibm.trl.BBM.mains.ForwardModel;
import com.ibm.trl.BBM.mains.GlobalParameter;
import com.ibm.trl.BBM.mains.StatusHolder;
import com.ibm.trl.BBM.mains.ForwardModel.Pack;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;
import com.ibm.trl.BBM.mains.StatusHolder.EEE;

import ibm.ANACONDA.Core.MatrixUtility;
import ibm.ANACONDA.Core.MyMatrix;

public class SafetyScoreEvaluator2 {

	static final Random rand = new Random();
	static final int numField = GlobalParameter.numField;
	static final ForwardModel fm = new ForwardModel();

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 *
	 */

	public static class HashTable {
		static MessageDigest md;
		static {
			try {
				md = MessageDigest.getInstance("SHA-512");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void samplingTouch(int numTry, Pack pack, int me, int[][] actionTargetSeq, double[][][] touch) throws Exception {
		Pack packNow = pack;

		//////////////////////////////////////////////////////////////////
		// �S�A�N�V������SurvivableScore���v�Z����B
		//////////////////////////////////////////////////////////////////
		int numt = actionTargetSeq[0].length;

		for (int tryIndex = 0; tryIndex < numTry; tryIndex++) {
			Pack packNext = packNow;
			for (int t = 0; t < numt; t++) {
				boolean[][] bombExist = new boolean[numField][numField];
				for (EEE bbb : packNext.sh.getBombEntry()) {
					bombExist[bbb.x][bbb.y] = true;
				}

				// ��肤��A�N�V������񋓂��āA��������B
				int[] actions = new int[4];
				for (int ai = 0; ai < 4; ai++) {
					if (actionTargetSeq[ai][t] == -1) {
						actions[ai] = rand.nextInt(6);
					} else {
						actions[ai] = actionTargetSeq[ai][t];
					}
				}

				packNext = fm.Step(packNext.board, packNext.abs, packNext.sh, actions);
			}

			// ���B�����ꏊ�𑫂����ށB
			for (AgentEEE aaa : packNext.sh.getAgentEntry()) {
				touch[aaa.agentID - 10][aaa.x][aaa.y]++;
			}
		}
	}
	
	static class Result {
		int actMe = -1;
		List<Pack> packs = new ArrayList<Pack>();
		List<Integer> acts = new ArrayList<Integer>();
	}
	
	static private boolean checkAllComb(Pack pack, int me, int target, int depth, int xminOrg, int xmaxOrg, int yminOrg, int ymaxOrg, LinkedList<Integer> actmeseq) throws Exception {

		for (int actme = 0; actme < 6; actme++) {

			// System.out.println("depth=" + depth + ", actme=" + actme);

			int xmin = xminOrg;
			int xmax = xmaxOrg;
			int ymin = yminOrg;
			int ymax = ymaxOrg;

			// �^�[�Q�b�g�̃A�N�V������S�p�^�[�����ׂ�B
			Pack[] packs = new Pack[6];
			for (int act = 0; act < 6; act++) {
				// for (int act = 4; act >= 1; act--) {
				int[] actions = new int[4];
				actions[me - 10] = actme;
				actions[target - 10] = act;
				packs[act] = fm.Step(pack.board, pack.abs, pack.sh, actions);
			}

			// �ړ��\�͈͂𒲂ׂ�B
			for (int act = 0; act < 6; act++) {
				// for (int act = 4; act >= 1; act--) {
				Pack pack2 = packs[act];
				for (AgentEEE aaa : pack2.sh.getAgentEntry()) {
					if (aaa.agentID != target) continue;
					xmax = Math.max(xmax, aaa.x);
					xmin = Math.min(xmin, aaa.x);
					ymax = Math.max(ymax, aaa.y);
					ymin = Math.min(ymin, aaa.y);
				}
			}

			int[][][] pos = new int[6][4][2];
			for (int act = 0; act < 6; act++) {
				// for (int act = 4; act >= 1; act--) {
				Pack pack2 = packs[act];
				for (AgentEEE aaa : pack2.sh.getAgentEntry()) {
					pos[act][aaa.agentID - 10][0] = aaa.x;
					pos[act][aaa.agentID - 10][1] = aaa.y;
				}
			}

			// if (countAlive == 0) {
			// System.out.println("�E�����I�I�I");
			// }

			// �\���Ȑ[���܂ŒT�����Ă�����A�����_�ł̕������߂̉ۂ�Ԃ��B
			if (depth >= 3) {
				if (xmin == xmax || ymin == ymax) {
					return true;
				} else {
					continue;
				}
			} else {
				// �[��������Ȃ��ꍇ�A�[�@��p��

				// ���̎��_�ŕ������߂Ɏ��s���Ă���B�����܂ł̃A�N�V�����n��͕������ߎ��s�B
				if (xmax > xmin && ymax > ymin) {
					continue;
				}

				// �A�N�V�����̌��ʂ������ɂȂ���͕̂Е��ȗ�����B
				Set<Integer> selected = new TreeSet<Integer>();
				for (int act = 0; act < 6; act++) {
					boolean find = false;
					for (int act2 = 0; act2 < act; act2++) {

						boolean samePos = true;
						for (int i = 0; i < 4; i++) {
							if (pos[act][i][0] != pos[act2][i][0] || pos[act][i][1] != pos[act2][i][1]) {
								samePos = false;
								break;
							}
						}
						if (samePos == false) continue;

						// double def = packs[act].board.minus(packs[act2].board).normL1();
						// if (def > 0) continue;

						boolean sameAbility = true;
						for (int ai = 0; ai < 4; ai++) {
							if (packs[act].abs[ai].equals(packs[act2].abs[ai]) == false) {
								sameAbility = false;
								break;
							}
						}
						if (sameAbility == false) continue;

						find = true;
						break;
					}
					if (find) continue;
					selected.add(act);
				}

				boolean successAll = true;
				for (int act : selected) {
					// System.out.println("depth=" + depth + ", actme=" + actme + ", act=" + act);

					actmeseq.addLast(actme);
					boolean success = checkAllComb(packs[act], me, target, depth + 1, xmin, xmax, ymin, ymax, actmeseq);
					actmeseq.removeLast();
					if (success == false) {
						successAll = false;
						break;
					}
				}

				if (successAll) {
					if (depth == 0) {
						System.out.println("�������ߐ����I�I");
						System.out.print("action seq");
						for (int temp : actmeseq) {
							System.out.print(", " + temp);
						}
						System.out.print(", " + actme);
						System.out.println();
					}
					// System.out.println("����̑S�A�N�V�����ɑ΂��ĕ������ߐ���, " + depth + ", " + actme);
					return true;
				}
			}
		}

		// �S�A�N�V�����ŕ������߂Ɏ��s�����B
		return false;
	}

	static private void test_003_r(Pack pack, int me, int depth, int[][] seq) throws Exception {

		// ���肪��������Ă����C���ɑ����Ă���ꍇ�́A����1�X�e�b�v�ǐՂ���B
		AgentEEE[] agents = new AgentEEE[4];
		for (AgentEEE aaa : pack.sh.getAgentEntry()) {
			agents[aaa.agentID - 10] = aaa;
		}

		int[][] seq2 = new int[4][depth + 1];
		for (int ai = 0; ai < 4; ai++) {
			for (int d = 0; d < depth; d++) {
				seq2[ai][d] = seq[ai][d];
			}
		}

		// ���� vs �G�ŁA�G���ǂ̂悤�ȓ��̓V�[�P���X�����Ă��A�c�����E�������ɐ����ꏊ���Œ�ł���ꍇ�́A1�X�e�b�v���g���b�L���O���Ă݂�B
		boolean[][] flagX = new boolean[4][6];
		boolean[][] flagY = new boolean[4][6];

		for (int actMe = 0; actMe < 6; actMe++) {
			double[][][] touch = new double[4][numField][numField];

			for (int ai2 = 0; ai2 < 4; ai2++) {
				if (ai2 == me - 10) {
					seq2[ai2][depth] = actMe;
				} else {
					seq2[ai2][depth] = -1;
				}
			}
			samplingTouch(100, pack, me, seq2, touch);

			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				AgentEEE aaa = agents[ai];

				// �����C������
				{
					boolean flag = true;
					int max = 0;
					int min = numField;
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							if (touch[ai][x][y] == 0) continue;

							if (x != aaa.x) {
								flag = false;
								break;
							}

							int pos = y - aaa.y;

							if (pos > max) {
								max = pos;
							}
							if (pos < min) {
								min = pos;
							}
						}
						if (flag == false) break;
					}
					flagY[ai][actMe] = flag;
				}

				// �c���C������
				{
					boolean flag = true;
					int max = 0;
					int min = numField;
					for (int x = 0; x < numField; x++) {
						for (int y = 0; y < numField; y++) {
							if (touch[ai][x][y] == 0) continue;

							if (y != aaa.y) {
								flag = false;
								break;
							}

							int pos = x - aaa.x;

							if (pos > max) {
								max = pos;
							}
							if (pos < min) {
								min = pos;
							}
						}
						if (flag == false) break;
					}
					flagX[ai][actMe] = flag;
				}
			}
		}

		// ���C����������ꍇ�́A���̐[�@��A�N�V���������߂�B
		MyMatrix board2 = new MyMatrix(pack.board);
		for (int ai = 0; ai < 4; ai++) {
			board2.data[agents[ai].x][agents[ai].y] = Constant.Passage;
		}
		MyMatrix dis = BBMUtility.ComputeOptimalDistance(board2, agents[me - 10].x, agents[me - 10].y, Integer.MAX_VALUE);

		Set<Integer> next = new TreeSet<Integer>();
		{
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (dis.data[agents[ai].x][agents[ai].y] > 1000) continue;
				int numT = 0;
				int numF = 0;
				for (int act = 0; act < 6; act++) {
					if (flagY[ai][act]) numT++;
					else numF++;
				}
				// �����̃A�N�V�����ɂ���ă��C�������̉ۂ��ς��ꍇ
				if (numT > 0 && numF > 0) {
					for (int act = 0; act < 6; act++) {
						if (flagY[ai][act]) {
							next.add(act);
						}
					}
				}
				// ��������Ă����C����������ꍇ�B
				if (numT == 6) {
					int dir = BBMUtility.ComputeFirstDirection(dis, agents[ai].x, agents[ai].y);
					next.add(dir);
				}
			}
		}

		{
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (dis.data[agents[ai].x][agents[ai].y] > 1000) continue;
				int numT = 0;
				int numF = 0;
				for (int act = 0; act < 6; act++) {
					if (flagX[ai][act]) numT++;
					else numF++;
				}
				// �����̃A�N�V�����ɂ���ă��C�������̉ۂ��ς��ꍇ
				if (numT > 0 && numF > 0) {
					for (int act = 0; act < 6; act++) {
						if (flagY[ai][act]) {
							next.add(act);
						}
					}
				}
				// ��������Ă����C����������ꍇ�B
				if (numT == 6) {
					int dir = BBMUtility.ComputeFirstDirection(dis, agents[ai].x, agents[ai].y);
					next.add(dir);
				}
			}
		}

		for (int n : next) {
			seq2[me - 10][depth] = n;
			test_003_r(pack, me, depth + 1, seq2);
		}
	}

	static private void test_003(Pack pack, int me) throws Exception {
		System.out.println("aaabbbcccddd");

		AgentEEE[] agents = new AgentEEE[4];
		for (AgentEEE aaa : pack.sh.getAgentEntry()) {
			agents[aaa.agentID - 10] = aaa;
		}

		AgentEEE agentMe = agents[me - 10];

		MyMatrix board2 = new MyMatrix(pack.board);
		for (EEE eee : pack.sh.getAgentEntry()) {
			board2.data[eee.x][eee.y] = Constant.Passage;
		}
		MyMatrix dis = BBMUtility.ComputeOptimalDistance(board2, agentMe.x, agentMe.y, Integer.MAX_VALUE);

		for (int ai = 0; ai < 4; ai++) {
			if (ai == me - 10) continue;
			AgentEEE aaa = agents[ai];
			if (aaa == null) continue;
			int d = (int) dis.data[aaa.x][aaa.y];
			if (d >= 3) continue;

			int xmax = 0;
			int xmin = numField;
			int ymax = 0;
			int ymin = numField;
			LinkedList<Integer> myactseq = new LinkedList<Integer>();
			checkAllComb(pack, me, ai + 10, 0, xmin, xmax, ymin, ymax, myactseq);
		}
	}


	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * @param numTry
	 * @param pack
	 * @param me
	 * @param actionTargetSeq
	 * @return
	 * @throws Exception
	 */

	public static double[][] sampling(int numTry, Pack pack, int me, int[][] actionTargetSeq) throws Exception {
		Pack packNow = pack;

		//////////////////////////////////////////////////////////////////
		// �S�A�N�V������SurvivableScore���v�Z����B
		//////////////////////////////////////////////////////////////////
		int numt = actionTargetSeq[0].length;

		double[] points = new double[4];
		double[] pointsTotal = new double[4];
		double[][][] touch = new double[4][numField][numField];

		for (int tryIndex = 0; tryIndex < numTry; tryIndex++) {
			Pack packNext = packNow;
			for (int t = 0; t < numt; t++) {
				boolean[][] bombExist = new boolean[numField][numField];
				for (EEE bbb : packNext.sh.getBombEntry()) {
					bombExist[bbb.x][bbb.y] = true;
				}

				// ��肤��A�N�V������񋓂��āA��������B
				int[] actions = new int[4];
				for (int ai = 0; ai < 4; ai++) {
					if (actionTargetSeq[ai][t] == -1) {
						actions[ai] = rand.nextInt(6);
					} else {
						actions[ai] = actionTargetSeq[ai][t];
					}
				}

				packNext = fm.Step(packNext.board, packNext.abs, packNext.sh, actions);
			}

			// ���B�����ꏊ�𑫂����ށB
			for (AgentEEE aaa : packNext.sh.getAgentEntry()) {
				touch[aaa.agentID - 10][aaa.x][aaa.y]++;
			}

			// �����Ă邩�ǂ����𐔂���B
			for (int ai = 0; ai < 4; ai++) {
				if (packNow.abs[ai].isAlive == false) continue;

				double ppp;
				if (packNext.abs[ai].isAlive == false) {
					ppp = 0;
				} else {
					ppp = 1;
				}
				pointsTotal[ai] += 1;
				points[ai] += 1 * ppp;
			}
		}

		// ���B�\�ӏ��𐔂���B
		double[] counter = new double[4];
		{
			for (int ai = 0; ai < 4; ai++) {
				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						if (touch[ai][x][y] > 0) {
							counter[ai]++;
						}
					}
				}
			}
		}

		double[][] ret = new double[3][];
		ret[0] = points;
		ret[1] = pointsTotal;
		ret[2] = counter;
		return ret;
	}

	static private void test_001_r(Pack pack, int me, List<Integer> actionSeq) throws Exception {

//		if (actionSeq.size() >= 2) {
//
//			for (int a : actionSeq) {
//				System.out.print("," + a);
//			}
//			System.out.println();
//
//			int[] actionTargetSeq = new int[actionSeq.size() + 10];
//			for (int i = 0; i < actionTargetSeq.length; i++) {
//				actionTargetSeq[i] = -1;
//			}
//			for (int i = 0; i < actionSeq.size(); i++) {
//				actionTargetSeq[i] = actionSeq.get(i);
//			}
//
//			double[][] yyy = new double[4][6];
//			for (int act = 0; act < 6; act++) {
//				actionTargetSeq[actionSeq.size()] = act;
//				double[][] ret = sampling(100, pack, me, actionTargetSeq);
//				double[] moveCounter = ret[2];
//				for (int ai = 0; ai < 4; ai++) {
//					yyy[ai][act] = moveCounter[ai];
//				}
//			}
//
//			return;
//		}
//
//		int[] actionTargetSeq = new int[actionSeq.size() + 1];
//		for (int i = 0; i < actionSeq.size(); i++) {
//			actionTargetSeq[i] = actionSeq.get(i);
//		}
//
//		double[][] yyy = new double[4][6];
//		for (int act = 0; act < 6; act++) {
//			actionTargetSeq[actionSeq.size()] = act;
//			double[][] ret = sampling(36, pack, me, actionTargetSeq);
//			double[] moveCounter = ret[2];
//			for (int ai = 0; ai < 4; ai++) {
//				yyy[ai][act] = moveCounter[ai];
//			}
//		}
//
//		// �Ȃ񂩂�������𔭌�����B
//		for (int ai = 0; ai < 4; ai++) {
//			if (pack.abs[ai].isAlive == false) continue;
//			if (ai == me - 10) continue;
//			double min = Double.MAX_VALUE;
//			double max = -Double.MAX_VALUE;
//			for (int act = 0; act < 6; act++) {
//				if (yyy[ai][act] < min) {
//					min = yyy[ai][act];
//				}
//				if (yyy[ai][act] > max) {
//					max = yyy[ai][act];
//				}
//			}
//			if (max > 10) {
//				// System.out.println("�L�͈͂Ɉړ��\");
//			}
//			if (max > 6 && min <= 1) {
//				System.out.println("�Ȃ񂩂�����");
//			}
//		}
//
//		// �s���悪�i���A�N�V�������W�߂�B
//		Set<Integer> as = new TreeSet<Integer>();
//		for (int ai = 0; ai < 4; ai++) {
//			if (pack.abs[ai].isAlive == false) continue;
//			if (ai == me - 10) continue;
//
//			double min = Double.MAX_VALUE;
//			double max = -Double.MAX_VALUE;
//			for (int act = 0; act < 6; act++) {
//				if (yyy[ai][act] < min) {
//					min = yyy[ai][act];
//				}
//				if (yyy[ai][act] > max) {
//					max = yyy[ai][act];
//				}
//			}
//
//			for (int act = 0; act < 6; act++) {
//				if (yyy[ai][act] <= 1 && max >= 2) {
//					// if (yyy[ai][act] <= 1) {
//					as.add(act);
//				}
//			}
//		}
//
//		// �s���悪�i���A�N�V�����ɑ΂��āA1�X�e�b�v�T������B
//		for (int act : as) {
//			List<Integer> temp = new ArrayList<Integer>();
//			temp.addAll(actionSeq);
//			temp.add(act);
//			test_001_r(pack, me, temp);
//		}

		// for (int act = 0; act < 6; act++) {
		// actionTargetSeq[actionSeq.size()] = act;
		// double[][] ret = sampling(100, pack, me, actionTargetSeq);
		//
		// // �J�E���^�[��1�Ɍ���ł���ꍇ�́A�~�߂�B
		// double[] moveCounter = ret[2];
		// for (int ai = 0; ai < 4; ai++) {
		// if (ai == me - 10) continue;
		// if (moveCounter[ai] <= 3) {
		// if (actionSeq.size() >= 4) {
		// System.out.println("�Ȃ񂩂�����");
		// }
		// List<Integer> temp = new ArrayList<Integer>();
		// temp.addAll(actionSeq);
		// temp.add(act);
		// test_001_r(pack, me, temp);
		// }
		// }
		// }

	}

	static private void test_001(Pack pack, int me) throws Exception {
		List<Integer> temp = new ArrayList<Integer>();
		test_001_r(pack, me, temp);
	}

	static private void test_002(Pack pack, int me) throws Exception {
		int numt = 12;
		int[][] actionTargetSeq = new int[4][numt];
		for (int ai = 0; ai < 4; ai++) {
			for (int i = 0; i < actionTargetSeq.length; i++) {
				actionTargetSeq[ai][i] = -1;
			}
		}

		double[][][] yy = new double[4][6][6];
		double[][][] sf = new double[4][6][6];
		for (int act = 0; act < 6; act++) {
			actionTargetSeq[3][0] = act;
			for (int act2 = 0; act2 < 6; act2++) {
				actionTargetSeq[3][1] = act2;
				double[][] ret = sampling(500, pack, me, actionTargetSeq);

				double[] moveCounter = ret[2];
				for (int ai = 0; ai < 4; ai++) {
					yy[ai][act][act2] = moveCounter[ai];
				}

				for (int ai = 0; ai < 4; ai++) {
					sf[ai][act][act2] = ret[0][ai] / ret[1][ai];
				}
			}
		}
		//
		System.out.println("===sf===");
		MatrixUtility.OutputMatrix(new MyMatrix(sf[2]));
		System.out.println("===sf===");
		MatrixUtility.OutputMatrix(new MyMatrix(sf[3]));

		System.out.println("===yy===");
		MatrixUtility.OutputMatrix(new MyMatrix(yy[2]));
		System.out.println("===yy===");
		MatrixUtility.OutputMatrix(new MyMatrix(yy[3]));

		for (int act = 0; act < 6; act++) {
			for (int act2 = 0; act2 < 6; act2++) {
				if (yy[2][act][act2] == 0 && yy[3][act][act2] >= 1) {
					System.out.println("???");
				}
			}
		}

		System.out.println("===END===");

	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * @author takahasi
	 *
	 */

//	static class Result {
//		double scoreBest = Double.MAX_VALUE;
//		int[] actionSeqBest = null;
//	}

	private static void rrr(int numTry, Pack pack, int me, int you, int depth, int maxDepth, int[] actionSeq, Result result) throws Exception {
//		if (depth == maxDepth) {
//			double[][] temp = sampling(numTry, pack, me, actionSeq);
//
//			double sfMe = temp[0][me - 10] / temp[1][me - 10];
//			double sfYou = temp[0][you - 10] / temp[1][you - 10];
//
//			if (sfMe > 0.1 && sfYou < result.scoreBest) {
//				result.scoreBest = sfYou;
//				result.actionSeqBest = new int[actionSeq.length];
//				for (int i = 0; i < actionSeq.length; i++) {
//					result.actionSeqBest[i] = actionSeq[i];
//				}
//			}
//		} else {
//			for (int act = 0; act < 6; act++) {
//				actionSeq[depth] = act;
//				rrr(numTry, pack, me, you, depth + 1, maxDepth, actionSeq, result);
//			}
//		}
	}

	// public static double[][][] evaluateSafetyScore_temp(int numTry, Pack pack, int me) throws Exception {
	// numTry = 300;
	//
	// AgentEEE agentMe = null;
	// for (AgentEEE aaa : pack.sh.getAgentEntry()) {
	// if (aaa.agentID == me) {
	// agentMe = aaa;
	// }
	// }
	// if (agentMe == null) return null;
	//
	// // ���ڂ��Ă���G�[�W�F���g��ǂ��l�߂�p�X�����邩�ǂ������ׂ�B
	// for (AgentEEE aaa : pack.sh.getAgentEntry()) {
	// if (aaa.agentID == me) continue;
	//
	// // int numWall = BBMUtility.numWall(pack.board, aaa.x, aaa.y);
	// // if (numWall != 3) continue;
	//
	// // MyMatrix board2 = new MyMatrix(pack.board);
	// // board2.data[aaa.x][aaa.y] = Constant.Passage;
	// // MyMatrix dis = BBMUtility.ComputeOptimalDistance(board2, agentMe.x, agentMe.y, Integer.MAX_VALUE);
	// // int d = (int) dis.data[aaa.x][aaa.y];
	// // if (d > 2) continue;
	//
	// for (int numDepth = 2; numDepth < 3; numDepth++) {
	// Result result = new Result();
	// int[] actionSeq = new int[numDepth];
	// rrr(numTry, pack, me, aaa.agentID, 0, numDepth, actionSeq, result);
	//
	// System.out.println("################, " + aaa.agentID + ", " + result.scoreBest);
	//
	// if (result.scoreBest < 0.01) {
	// System.out.print(numDepth + "�X�e�b�v�Œǂ��l�߉\�B");
	// for (int a : result.actionSeqBest) {
	// System.out.print(", " + a);
	// }
	// System.out.println();
	// System.out.println();
	// }
	// }
	// }
	// return null;
	// }

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * @param pack
	 * @param me
	 * @throws Exception
	 */
	static public void computeSafetyScore(Pack pack, int me) throws Exception {
		// test_001(pack, me);
		// test_002(pack, me);
//		 test_003(pack, me);
	}
}
