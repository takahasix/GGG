package com.ibm.trl.BBM.mains;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.Random;

import com.ibm.trl.BBM.mains.OptimalActionFinder.OAFParameter;

import ibm.ANACONDA.Core.MyMatrix;

public class GlobalParameter {
	static Random rand = new Random();

	static public String PID;
	static public int numThread = 1;
	static final public int numField = 11;
	static final public boolean verbose = false;

	static public OAFParameter oafparameterBest;
	static public OAFParameter[] oafparameters = new OAFParameter[4];

	static {
		try {
			// PID���l�����Ă����B
			{
				PID = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
				System.out.println("PID = " + PID);
			}

			// �X���b�h����ݒ肷��B
			{
				if (new File("data/parameters.txt").exists()) {
					Properties p = new Properties();
					p.load(new FileInputStream(new File("data/parameters.txt")));
					numThread = Integer.parseInt(p.getProperty("numThread"));
				} else {
					numThread = 1;
				}
				System.out.println("numThread = " + numThread);
			}

			// OAFParameter���ۑ�����Ă�����A�ǂݍ���ł����B
			{
				File file = new File("data/oafparameter.dat");
				if (file.exists()) {
					ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
					oafparameterBest = (OAFParameter) ois.readObject();
					ois.close();
				} else {
					oafparameterBest = new OAFParameter();
					oafparameterBest.numEpisode = 1;
					oafparameterBest.numFrame = 1;
				}

				for (int ai = 0; ai < 4; ai++) {
					oafparameters[ai] = oafparameterBest;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * Episode�I�����ɌĂ΂��B KPI���W�v����B ������xEpisode�����܂�����A�ȉ������B
	 * 
	 * �P�DGlobalParameter��OAPParameter��KPI�Ɣ�r���āA�ǂ����GlobalParamter�ɓo�^����B���łɃt�@�C���ɂ��ۑ�����B
	 * �Q�DGlobalParameter��OAFParameter�̐ݒ�������_���ɓ������āA�V�K�g���COAFParameter��ݒ肷��B
	 */
	static public void FinishOneEpisode(int me, double numFrame, double reward, double numItemGet) throws Exception {
		OAFParameter param = oafparameters[me - 10];
		param.numEpisode++;
		param.numFrame += numFrame;
		param.numItemGet += numItemGet;
		if (reward == 1) param.numWin++;

		if (param.numEpisode >= 100) {
			double stepSize = 0.01;

			///////////////////////////////////////////////////////////
			// KPI��Item�擾���̏ꍇ
			double score = param.numItemGet / param.numFrame;
			double scoreBest = GlobalParameter.oafparameterBest.numItemGet / GlobalParameter.oafparameterBest.numFrame;
			if (GlobalParameter.oafparameterBest.numFrame == 0 || score > scoreBest) {
				oafparameterBest = param;
				File file = new File("data/oafparameter.dat");
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
				oos.writeObject(oafparameterBest);
				oos.flush();
				oos.close();

				if (verbose) {
					System.out.println("found better parameter!!");
					System.out.println(String.format("score=%f, numEpisode=%f, numFrame=%f, numItemGet=%f, numWin=%f", score, param.numEpisode, param.numFrame, param.numItemGet, param.numWin));
					System.out.println(param.Keisu);

				}
			}

			// �p�����[�^���U�炷�B
			int[] targetIndexSet = { 0, 1, 2, 3, 5 };
			int index = -1;
			int dim = -1;
			boolean increment;
			while (true) {
				int i = rand.nextInt(targetIndexSet.length);
				dim = rand.nextInt(3);
				increment = rand.nextBoolean();
				index = targetIndexSet[i];
				if (param.KeisuUsed[index][dim]) {
					if (increment) {
						break;
					} else {
						if (param.Keisu.data[index][dim] != 0) {
							break;
						}
					}
				}
			}

			MyMatrix Keisu = new MyMatrix(GlobalParameter.oafparameterBest.Keisu);
			if (increment) {
				Keisu.data[index][dim] += stepSize;
			} else {
				Keisu.data[index][dim] -= stepSize;
				if (Keisu.data[index][dim] < 0) Keisu.data[index][dim] = 0;
			}

			oafparameters[me - 10] = new OAFParameter(Keisu);
		}
	}
}
