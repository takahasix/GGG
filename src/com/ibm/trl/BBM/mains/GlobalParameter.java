package com.ibm.trl.BBM.mains;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.ibm.trl.BBM.mains.OptimalActionFinder.OAFParameter;

import ibm.ANACONDA.Core.MyMatrix;

public class GlobalParameter {
	static Random rand = new Random();
	static NormalDistribution nd = new NormalDistribution();
	static final public boolean verbose = true;
	static final int timeStampling = 200;
	static final boolean isOptimizeParameter = false;

	static public String PID;
	static public int numThread = 1;
	static final public int numField = 11;

	static public OAFParameter[] oafparameters;
	static public OAFParameter oafparamCenter;
	static public MyMatrix KeisuGlobal;

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

			{
				File file = new File("data/oafparameter_average.dat");
				if (file.exists()) {
					ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
					KeisuGlobal = (MyMatrix) ois.readObject();
					ois.close();
					oafparamCenter = new OAFParameter(KeisuGlobal);
				} else {
					oafparamCenter = new OAFParameter();
					KeisuGlobal = new MyMatrix(oafparamCenter.Keisu);
				}
				oafparamCenter.numEpisode = 1;
				oafparamCenter.numFrame = 1;

				oafparameters = new OAFParameter[4];
				for (int ai = 0; ai < 4; ai++) {
					oafparameters[ai] = new OAFParameter(KeisuGlobal);
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
		OAFParameter oafparam = oafparameters[me - 10];
		oafparam.numEpisode++;
		oafparam.numFrame += numFrame;
		oafparam.numItemGet += numItemGet;
		if (reward == 1) oafparam.numWin++;

		if (isOptimizeParameter && oafparam.numEpisode >= 10) {
			double stepSize = 0.01;

			///////////////////////////////////////////////////////////
			// KPI��Item�擾���̏ꍇ
			double score = oafparam.numWin + oafparam.numItemGet * 0.1;
			double scoreCenter = oafparamCenter.numWin + oafparamCenter.numItemGet * 0.1;
			double max = Math.max(score, scoreCenter);
			score = score / max;
			scoreCenter = scoreCenter / max;
			score = Math.pow(score, 10);
			scoreCenter = Math.pow(scoreCenter, 10);

			System.out.println("���̋N�_OAFParameter");
			System.out.println(oafparamCenter.Keisu);
			System.out.println("������OAFParameter");
			System.out.println(oafparam.Keisu);
			System.out.println("����");
			System.out.println(oafparam.Keisu.minus(oafparamCenter.Keisu));
			System.out.println("���ʂ́A");
			System.out.println(score + " vs " + scoreCenter + "�i�N�_�j");

			boolean accept;
			if (rand.nextDouble() * scoreCenter < score) {
				oafparamCenter = oafparam;
				accept = true;
				System.out.println("�V����OAFParameter���󗝂����B");
			} else {
				accept = false;
				System.out.println("�V����OAFParameter�����p�����B");
			}
			System.out.println(String.format("score=%f, numEpisode=%f, numFrame=%f, numItemGet=%f, numWin=%f, accept=%b", score, oafparam.numEpisode, oafparam.numFrame, oafparam.numItemGet,
					oafparam.numWin, accept));

			// �O���[�o���̌W����������Ɠ������B
			double rate = 0.95;
			KeisuGlobal = KeisuGlobal.times(rate).plus(oafparamCenter.Keisu.times(1 - rate));

			// �ۑ�����B
			{
				File file = new File("data/oafparameter_average.dat");
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
				oos.writeObject(KeisuGlobal);
				oos.flush();
				oos.close();
			}

			// �p�����[�^���U�炷�B
			MyMatrix Keisu = new MyMatrix(oafparamCenter.Keisu);
			for (int ii = 0; ii < 3; ii++) {
				int numt = Keisu.numt;
				int numd = Keisu.numd;
				int index = -1;
				int dim = -1;
				boolean increment;
				while (true) {
					index = rand.nextInt(numt);
					dim = rand.nextInt(numd);
					increment = rand.nextBoolean();
					if (OAFParameter.KeisuUsed[index][dim]) {
						if (increment) {
							break;
						} else {
							if (Keisu.data[index][dim] != 0) {
								break;
							}
						}
					}
				}

				if (increment) {
					Keisu.data[index][dim] += stepSize * Math.abs(nd.sample());
				} else {
					Keisu.data[index][dim] -= stepSize * Math.abs(nd.sample());
					if (Keisu.data[index][dim] < 0) Keisu.data[index][dim] = 0;
				}
			}

			oafparam = new OAFParameter(Keisu);

			System.out.println("���̋N�_OAFParameter");
			System.out.println(oafparamCenter.Keisu);
			System.out.println("���Ɏ���OAFParameter");
			System.out.println(oafparam.Keisu);
			System.out.println("����");
			System.out.println(oafparam.Keisu.minus(oafparamCenter.Keisu));
			System.out.println("���ς�OAFParameter");
			System.out.println(KeisuGlobal);

			oafparameters[me - 10] = oafparam;
		}
	}
}
