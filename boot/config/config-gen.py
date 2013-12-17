from __future__ import print_function
import shutil

def main():
    algos = [
            {'name': 'RSLBench.Algorithms.BMS.BinaryMaxSum', 'time': 300000},
            {'name': 'RSLBench.Algorithms.MS.MaxSum', 'time': 300000},
            {'name': 'RSLBench.Algorithms.DSA.DSA', 'time': 300000},
    ]
    ks = [4]
    times = [90, 120]

    m = 'kobe'
    for k,t in [(k,t) for k in ks for t in times]:
        for a in algos:
            aname = a['name'].split('.')[-1]
            fname = m + '-jesus-infinito-' + aname + '-k' + str(k) + '-t' + str(t) + '.cfg'
            print('jesus-infinito-' + aname + '-k' + str(k) + '-t' + str(t))
            shutil.copy(m + '-base.cfg', fname)
            with open(fname, 'a') as f:
                print("", file=f)
                print("# When should agents start acting", file=f)
                print("experiment.start_time: " + str(t), file=f)
                print("", file=f)
                print("# MaxSum *and DSAFactorgraph* number of neighbors", file=f)
                print("maxsum.neighbors: " + str(k), file=f)
                print("", file=f)
                print("solver.class: " + a['name'], file=f)
                print("solver.time: " + str(a['time']), file=f)

                bs = [b for b in algos if b != a]
                for i,b in enumerate(bs):
                    i = i+1
                    print("solver." + str(i) + ".class: " + b['name'], file=f)
                    print("solver." + str(i) + ".time: " + str(b['time']), file=f)

                print("solver.3.class: RSLBench.Algorithms.Random.Random", file=f)
                print("solver.3.time: 100", file=f)
                print("solver.4.class: RSLBench.Algorithms.Greedy.Greedy", file=f)
                print("solver.4.time: 100", file=f)

if __name__ == '__main__':
    main()
