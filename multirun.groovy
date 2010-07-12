#!/usr/bin/env groovy

/*
cl = new CliBuilder(usage: '[groovy] multirun [-c concurrency] [-i iterations] [command]')

cl.c(longOpt: 'concurrency', 'The number of threads which will iterations. Default: 2.')
cl.i(longOpt: 'iterations', 'The number of iterations each thread should run. Default: 2.')

options = cl.parse(args)
*/

// TODO: Use CliBuilder to use switches instead of positional arguments, which suck
concurrency = args[0] as Integer
iterations = args[1] as Integer

lastArg = args.length - 1

command = args[2..lastArg].join(' ')

println "running $command $iterations times in each of $concurrency threads"

concurrency.times {
    Thread.start {
        iterations.times {
            process = command.execute()
            process.consumeProcessOutput(System.out, System.err)
            process.waitFor()
        }
    }
}