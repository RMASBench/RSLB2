from __future__ import print_function
import shutil

def main():
    run = 'ecai4'
    algos = [
            {'name': 'RSLBench.Algorithms.BMS.BinaryMaxSum', 'time': 300000},
            {'name': 'RSLBench.Algorithms.NewMS.MaxSum', 'time': 300000},
            {'name': 'RSLBench.Algorithms.DSA.DSA', 'time': 300000},
            {'name': 'RSLBench.Algorithms.Greedy.Greedy', 'time': 300000},
    ]
    ks = ['k3','k4','k5','np']
    times = [35]
    areas = ['a200']
    greedy = ['yes', 'no']

    m = 'paris'
    for k,t,area,g in [(k,t,area,g) for k in ks for t in times for area in areas for g in greedy]:
        for a in algos:
            aname = a['name'].split('.')[-1]

            if k == 'np' and aname == 'MaxSum':
                continue

            fname = m + '-' + run + '-' + aname + '-' + k + '-t' + str(t) + '-' + area \
                + '-g' + g + '.cfg'
            print(fname)
            shutil.copy(m + '-base.cfg', fname)
            with open(fname, 'a') as f:
                if k == 'np':
                    prune='false'
                    kk = k
                else:
                    kk = int(k[1])
                    prune='true'

                print("# Whether to make a sequential greedy pass through all agents at the end", file=f)
                print("dcop.greedy_correction: " + g, file=f)
                print("", file=f)
                print("# Amount of building area covered by a fire truck", file=f)
                print("util.fire_brigade_area: " + str(area[1:]), file=f)
                print("", file=f)
                print("# When should agents start acting", file=f)
                print("experiment.start_time: " + str(t), file=f)
                print("", file=f)
                print("# Whether to prune or not", file=f)
                print("problem.prune: " + prune, file=f)
                print("", file=f)
                print("# Pruned number of neighbors", file=f)
                print("problem.max_neighbors: " + str(kk), file=f)
                print("", file=f)
                print("solver.class: " + a['name'], file=f)
                print("solver.time: " + str(a['time']), file=f)

                bs = [b for b in algos if b != a and (b['name'] != 'RSLBench.Algorithms.NewMS.MaxSum' or k != 'np')]
                for i,b in enumerate(bs):
                    i = i+1
                    print("solver." + str(i) + ".class: " + b['name'], file=f)
                    print("solver." + str(i) + ".time: " + str(b['time']), file=f)


if __name__ == '__main__':
    main()
