#!/usr/bin/env groovy

/*
 * multirun, an Arc90 script
 * by Avi Flax <avif@arc90.com>
 * http://github.com/arc90/scripts/
 *
 * Copyright 2010 Arc90, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */ 

cli = new CliBuilder(usage: '[groovy] multirun[.groovy] [-c concurrency] [-i iterations] [-t timeout] [-p pause] [command]')

cli.with {
    c longOpt: 'concurrency', args: 1, argName: 'concurrency', 'The number of threads which will iterations. Default: 1.'
    i longOpt: 'iterations', args: 1, argName: 'iterations', 'The number of iterations each thread should run. Default: 1.'
    t longOpt: 'timeout', args: 1, argName: 'timeout', 'The number of seconds to wait until killing each process. 0 means no timeout. Default: 0.'
    p longOpt: 'pause', args:1, argName: 'pause', 'The number of seconds to pause between each iteration. Default: 0'
    v longOpt: 'verbose', 'If set, information will be output to standard error before and after each invocation of the command.'
    h longOpt: 'help', 'Display this message.'
}

opts = cli.parse(args)

if (!opts || opts.h || opts.arguments().size() == 0)
    return cli.usage()

command = opts.arguments().join(' ')

concurrency = opts.c ? opts.c as Integer : 1
iterations = opts.i ? opts.i as Integer : 1
timeout = opts.t ? opts.t as Integer : 0
pause = opts.p ? opts.p as Integer : 0
verbose = opts.v as Boolean

if (verbose)
    System.err.println "[multirun] running $command $iterations times in each of $concurrency threads"

Process.metaClass.getPid = {
    // from: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4244896   
    // it's too hard on Windows, oh well
    if (delegate.class.name == 'java.lang.UNIXProcess') {
        try {
            f = delegate.class.getDeclaredField('pid')
            f.accessible = true
            return f.getInt(delegate) as String
        } catch (e) {}
    }

    return '<unknown>'
}

concurrency.times {
    Thread.start {
        iterations.times {

            // local vars
            def prefix, start, process, pid, duration, exitcode
            
            if (verbose) {
                prefix = "\n[multirun] thread ${Thread.currentThread().id}, iteration ${it+1}: "
                System.err.println prefix + 'starting'
            }
            
            start = System.currentTimeMillis()  

            process = command.execute()

            process.consumeProcessOutput(System.out, System.err)
            
            if (verbose) {
                pid = process.pid
                System.err.println prefix + "spawned process $pid"
            }
            
            timeout ? process.waitForOrKill(timeout * 1000) : process.waitFor()

            duration = System.currentTimeMillis() - start

            System.out.flush()
            System.err.flush()
            
            try {
                exitcode = process.exitValue()
            } catch (e) {
                exitcode = 9999
            }
            
            if (verbose && timeout && exitcode == 143)
                System.err.println prefix + "process $pid killed after $duration MS (probably because of timeout)"
            else if (verbose)
                System.err.println prefix + "process $pid exited with exit code $exitcode after $duration MS"
            
            if (pause && it + 1 < iterations)
                Thread.sleep(pause * 1000)
         }
    }
}