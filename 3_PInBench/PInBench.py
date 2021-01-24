import sys
import glob
import subprocess
import abc
import csv
import pprint
import logging
import time
import os
import argparse
import re
from threading import Timer

class BenchmarkEngine(object):
    """
    Overview file as class attribute.
    """
    ov_file = None

    def __init__(self, directory, extension, inference_engine, log_file_suffix, csv_file_suffix, file_overview_suffix, jar, csv_columns,
                 info_dict):
        self.directory = directory
        self.extension = extension
        self.inf_eng = inference_engine
        self.log_suffix = log_file_suffix
        self.csv_suffix = csv_file_suffix
        self.file_overview_suffix = file_overview_suffix
        self.jar = jar
        self.csv_cols = csv_columns
        self.info_dict = info_dict

    @abc.abstractmethod
    def get_start_message(self):
        """
        Returns a string describing the chosen framework to be benchmarked
        """

    def main_wrapper(self):
        """ 
        Method that wraps the sequence of functions. 
        """
        # -- 0. Check if framework file is there
        self.check_jar()

        # -- 1. Prep output files & logging
        time_str = time.strftime("%Y_%m_%d-%H_%M")
        log_file = time_str + self.log_suffix
        csv_file = time_str + self.csv_suffix
        overview_file = time_str + self.file_overview_suffix

        # Empty list for file prefixes which should be excluded due to timeouts.
        self.exclude_file_prefixes = []

        # needed for BLOG Engine
        # Source: https://stackoverflow.com/a/49202811
        for handler in logging.root.handlers[:]:
            logging.root.removeHandler(handler)

        logging.basicConfig(filename=log_file,
                            level=logging.DEBUG,
                            format='%(levelname)s -- %(asctime)s %(message)s',
                            datefmt='%m/%d/%Y %H:%M:%S')
        # Add handler for printing the logs on the console.
        logging.getLogger().addHandler(logging.StreamHandler())

        logging.info("Script call for reproducibility: > {0}\n".format(" ".join([x for x in sys.argv]))) 
        logging.debug("All settings used:") 
        for k,v in sorted(vars(args).items()): 
             logging.debug("{0}: {1}".format(k,v))

        #logging.info(self.get_start_message())
        #if java_xmx:
        #    logging.info("'-Xmx16384M' will be used to fix the allocated java heap space.")
        logging.info("Initialising output files: [%s] and [%s].", log_file, csv_file)

        # Create output csv (with times, probabilities, ...)
        output = open(csv_file, 'w')
        writer = csv.DictWriter(output, dialect='excel', fieldnames=self.csv_cols, lineterminator='\n', delimiter=",")
        writer.writeheader()

        # Create output log (for file overview)
        self.ov_file = open(overview_file, 'w')

        # -- 2. Find model files and iterate over them
        files = self.find_files_in_dir()
        curr_setting_string = self.extract_setting_string(files[0])
        self.subsequent_fail_counter = 0
        for j, fileI in enumerate(files):
            
            if self.extract_setting_string(fileI) != curr_setting_string:
                self.subsequent_fail_counter = 0
                curr_setting_string = self.extract_setting_string(fileI)
            logging.info("### File no. %d/%d - '%s'", j+1, len(files), fileI)
            if any([fileI.startswith(ef) for ef in self.exclude_file_prefixes]):
                logging.info("    Skipping file because of previous timeout / (memory) error for same but smaller model.")
                self.write_status_to_overview(fileI, "timeout or error skip")
            else:
                self.status_counts = {'success':0, 'errors':0, 'timeouts':0}
                self.handle_file(fileI, writer)
                output.flush()
                self.handle_file_status_counters(fileI)
            

        logging.info(">>> Finished - all work done!")
        output.close()
        self.ov_file.close()
        self.cleanup()
        self.finish_routine()

    def check_jar(self):
        """
        Aborts the script in case the needed jar / executable file cannot be found.
        """
        if not os.path.exists(self.jar):
            sys.exit("Did not find the needed executable / jar file. Should be named '{}'.".format(self.jar))
            

    def finish_routine(self):
        """
        Method that is called at the very end of a run (i.e., after all work is done.)
        """
        try: 
            from Minibot.minibot import MiniBot
        except:
            print("No final Telegram message sent. (Could not import MiniBot Telegram)")
        else:
            mb = MiniBot(False)
            msg = "Current PInBench run finished!\n"\
                "Directory <code>{}</code>".format(self.directory)
            mb.send_message(msg)
    
    def extract_setting_string(self, filename):
        return filename.split("#")[0]

    def handle_file_status_counters(self, filename):
        """
        Does all the status counter handling for a file.
        1.) Checks if the file's status stats are considered a "fail":
            -> When more timeouts and errors than successes.
        2.) Checks if two subsequent files have been "fails"
            -> Adds the setting string to the exclusion list.
        
        Arguments:
            filename {string} -- current file name
        """
        logging.info(" >> File summary: success: {}, timeouts: {}, errors: {}".format(self.status_counts['success'], self.status_counts['timeouts'], self.status_counts['errors']))
        if (self.status_counts['success'] < self.status_counts['timeouts'] + self.status_counts['errors']):
            self.subsequent_fail_counter += 1
            if self.subsequent_fail_counter == 2 and timeout_skip:
                current_setting_string = self.extract_setting_string(filename) + ("#" if "#" in filename else "")
                self.add_prefix_to_exclusion_list(current_setting_string)
                self.subsequent_fail_counter = 0
        else:
            self.subsequent_fail_counter = 0

    def add_prefix_to_exclusion_list(self, prefix):
        logging.info("    Adding settings prefix '{}' to be excluded for future files.".format(prefix))
        self.exclude_file_prefixes.append(prefix)

    def write_status_to_overview(self, filename, status, query=""):
        if query != "":
            message = '{}, "{}", {}\n'.format(filename, query, status)
        else:
            if combine_queries:
                message = "{}, {}\n".format(filename, status)
            else: 
                message = '{}, "{}", {}\n'.format(filename, "all queries", status)
        self.ov_file.write(message)
        self.ov_file.flush()

    def find_files_in_dir(self):
        """
        Method that finds all model files with the given extension in the given directory
        """
        files = glob.glob(self.directory + "/*." + self.extension)

        def natural_sort(l): 
            convert = lambda text: int(text) if text.isdigit() else text.lower() 
            alphanum_key = lambda key: [ convert(c) for c in re.split('([0-9]+)', key) ] 
            return sorted(l, key = alphanum_key)
        
        try: 
            sorted_files = natural_sort(files)
        except:
            sorted_files = files

        logging.info(str.format("Found {} {} file(s) in the selected directory: '{}'", len(sorted_files), self.extension,
                                self.directory))
        return sorted_files

    @abc.abstractmethod
    def extract_queries(self, file_string, filename, engine):
        """
        Method that does query processing. Implemented in the subclasses
        """

    @abc.abstractmethod
    def extract_information(self, cmd_output):
        """
        Method that extracts needed information from the command line output. Implemented in each subclass.
        """

    @abc.abstractmethod
    def cleanup(self):
        """
        Method that implements subclass specific cleanup tasks (e.g. deletes temporary files, ...)
        """

    @abc.abstractmethod
    def handle_file(self, filename, writer_obj):
        """
        Method that needs to be implemented in concrete subclasses.
        Handles the control whether the queries are done en bloc or one by one.

        Depending on variable 'combine_queries' (which is set according to the script argument).

        If one of the modes is not yet implemented, we create a overwrite the corresponding
        methods (handle_single_queries or handle_combined_queries) and abort there.

        :param filename: filename of the model file to be handled.
        :param writer_obj: writer_obj to write the extracted information to
        """
        if combine_queries:
            # Mode: Combine queries
            with open(filename, 'r') as f:
                file_string = f.read()
            queries = self.extract_queries(file_string, filename, self)
            infos = self.handle_combined_queries(queries, filename)

            for info in infos:
                if self.info_check_ok(info):
                    writer_obj.writerow(info)
                else:
                	logging.error("Info not okay (contains -1 value)")
        else:
            self.handle_single_queries(filename, writer_obj)
    
    def handle_single_queries(self, filename, writer_obj):
        """
        Standard wrapper method that handles information extraction for one file (e.g. splits up queries,
        performs console calls, ...) and saves the info to the writer_obj via writerow(info_dict)
        """ 
        with open(filename, 'r') as f:
            model_string = f.read()

        query_objects = self.extract_queries(model_string, filename, self)

        pp = pprint.PrettyPrinter(depth=6)
        # 3.) For each query: execute the forclift jar. Collect inference parameter information.
        for i, query in enumerate(query_objects):
            logging.info("    -- Query no.  {}/{}: '{}'".format(i + 1, len(query_objects), query))

            std_out, std_err = query.execute()

            if std_err == "<Timeout>":
                logging.warning("Timeout in script execution.")
                self.write_status_to_overview(filename, "timeout", query=query.queryString)
                self.status_counts['timeouts'] += 1
            elif self.check_for_error(std_out, std_err):
                try:
                    infos = self.extract_information(std_out)
                except Exception as e:
                    logging.error("Uncaught exception in information extraction:")
                    logging.error(e)
                    logging.error("std_out: "+std_out)
                    logging.error("std_err: "+std_err)
                    infos = self.info_dict.copy()
                infos['filename'] = str(filename)
                infos['query'] = str(query)
                if verbose:
                    print("> Extracted Information")
                    pp.pprint(infos)
                if self.info_check_ok(infos):
                    self.write_status_to_overview(filename, "all ok", query=query.queryString)
                    self.status_counts['success'] += 1
                    writer_obj.writerow(infos)
                else:
                    self.write_status_to_overview(filename, "error (info extraction)", query=query.queryString)
                    self.status_counts['errors'] += 1
            else:
                self.write_status_to_overview(filename, self.determine_error_type(std_err), query=query.queryString)
                logging.error("Error in script execution. Output:\n" + self.get_error_message(std_out, std_err).replace("\r\n\tat", "\n"))
                self.status_counts['errors'] += 1


    def handle_combined_queries(self, queries, filename):
        """
        Standard method that does the inference with combined queries.
        :param queries: List of queries
        :param filename: filename String
        :return: list of info dicts containing the extracted information
        """
        comp_string = ""
        for query in queries:
            # compose query string
            comp_string = ",".join([query.queryString for query in queries])

        logging.info("    -- Executing combined query string '{}' ".format(comp_string))

        std_out, std_err = self.execute_combined_query(comp_string,filename)

        if std_err == "<Timeout>":
                logging.warning("Timeout in script execution.")
                self.write_status_to_overview(filename, "timeout", query=query.queryString)
                self.status_counts['timeouts'] += 1
                return []
        elif self.check_for_error(std_out, std_err):
            # TODO: Create one info dict for each subquery
            try:
                infos = self.extract_information(std_out)
            except Exception as e:
                logging.error("Uncaught exception in information extraction:")
                logging.error(e)
                logging.error("std_out: "+std_out)
                logging.error("std_err: "+std_err)
                infos = self.info_dict.copy()
            infos['filename'] = str(filename)
            infos['query'] = str(comp_string)

            if self.info_check_ok(infos):
                self.write_status_to_overview(filename, "all ok")
                self.status_counts['success'] += 1
            else: 
                self.write_status_to_overview(filename, "error (info extraction)", query=query.queryString)
                self.status_counts['errors'] += 1
            if verbose:
                pp = pprint.PrettyPrinter(depth=6)
                pp.pprint(infos)
            return [infos]
        else:
            self.write_status_to_overview(filename, self.determine_error_type(std_err))
            logging.error("Error in script execution. Output:\n" + self.get_error_message(std_out, std_err).replace("\r\n\tat", "\n"))
            self.status_counts['errors'] += 1
            return []

    def info_check_ok(self, info_dict):
        """
        Checks a info dictionary if information extraction has worked as expected (i.e. none of the values are -1 - as initially set).
        Returns True if all if all is okay, False otherwise.
        """
        for key in info_dict:
            if info_dict[key] == -1:
                logging.error("-1 value in info_dict with key: '{}'".format(key))
                if verbose:
                    logging.error("Complete info_dict: {}".format(info_dict))
                return False
        return True


    @abc.abstractmethod
    def execute_combined_query(self, comp_string, filename):
        """
        Executes a combined query, represented by the comp_string as composed query string
        in the given file.
        """
        
    def determine_error_type(self, std_err):
        if "NegativeArraySizeException" in std_err:
            return "NegativeArraySizeException"
        elif "ArrayIndexOutOfBoundsException" in std_err:
            return "ArrayIndexOutOfBoundsException"
        elif "Java heap space" in std_err:
            return "OutOfMemoryError:JavaHeapSpace"
        else:
            return "unspecified error"

    @abc.abstractmethod
    def check_for_error(self, std_out, std_err):
        """
        Checks `std_out` and `std_err` if an error occurs. This might vary between frameworks (i.e. some might not
        use `std_err`).

        Return:
            True if all is good (= no error)
            False if error occured.
        """
    
    
    def get_error_message(self, std_out, std_err):
        """
        Extract error message from `std_out` and `std_err`.

        Return: 
            Error message
        """
        return std_err


class ForcliftEngine(BenchmarkEngine):
    def __init__(self, directory):
        # Class config
        inference_engine = ""
        extension = "mln"
        log_suffix = "_forclift.log"
        csv_suffix = '_forclift_times.csv'
        ov_suffix = '_forclift_overview.log'
        jar = 'forclift-3.1.jar'

        if combine_queries:
            csv_cols = ['filename', 'query', 'P(query)', 't_inference']

            info_dict = {'filename': '', 'query': '', 'P(query)': -1, 't_inference': -1}

        else:
            csv_cols = ['filename', 'query', 't_evidence', 't_query', 't_inference', 'ev_nnf_size',
                        'ev_smooth_nnf_size', 'q_nnf_size', 'q_smooth_nnf_size', 'Z', 'Z(query)',
                        'P(query)']

            info_dict = {'filename': '', 'query': '', 't_evidence': -1, 't_query': -1, 't_inference': -1,
                         'ev_nnf_size': -1, 'ev_smooth_nnf_size': -1, 'q_nnf_size': -1, 'q_smooth_nnf_size': -1,
                         'Z': -1, 'Z(query)': -1, 'P(query)': -1}

        super(ForcliftEngine, self).__init__(directory=directory,
                                             extension=extension,
                                             inference_engine=inference_engine,
                                             log_file_suffix=log_suffix,
                                             csv_file_suffix=csv_suffix,
                                             file_overview_suffix = ov_suffix,
                                             jar=jar,
                                             csv_columns=csv_cols,
                                             info_dict=info_dict)

    def get_start_message(self):
        return "Running benchmark on framework: [Forclift]"

    def handle_file(self, filename, writer_obj):
        if combine_queries:
            self.handle_margs_file(filename, writer_obj)
        else:
            super(ForcliftEngine, self).handle_single_queries(filename, writer_obj)

    def extract_information(self, fl_str):
        """
        Function that extracts relevant information from the forclift output string.
        Returns a dictionary containing the relevant information
        """
        info_dict = self.info_dict.copy()

        try:
            lines = fl_str[fl_str.index("[t_evidence]"):].split("\n")
        except:
            logging.warning("[t_evidence] not found in return string. \
            Skipping information extraction.")
        else:
            for line in [line for line in lines if line != ""]:
                key = line[line.find("[") + 1:line.find("]")]
                value = line[line.find("{") + 1:line.find("}")]
                info_dict[key] = value
        return info_dict

    def extract_queries(self, file_string, filename, engine):
        """
        Function that cleans up a list of queries
        (removes comment-double-slashes ('//') and  empty strings).
        """
        # Reduce fileString to relevant lines
        query_lines = file_string.split("[Queries]")[1].split("\n")[3:]
        # Extract raw query strings from query lines
        query_strings = [query[3:].strip() for query in query_lines if query.startswith("//")]

        query_objects = [ForcliftQuery(query_string, filename, engine) for query_string in query_strings]

        return query_objects

    def cleanup(self):
        # no cleanup necessary
        pass

    def handle_margs_file(self, filename, writer_obj):
        """
        Wrapper function for sequence of actions, if margs argument is present.

        1. Executes inference (on the ``filename`` file, no query needed, but with ``--margs`` argument! )
        2. Extracts information
        3. Writes extracted information to csv (via ``writer_obj.writerow(dict)``

        :param filename: filename of model file to use inference on
        :param writer_obj: writer object to write the extracted information
        """
        cmd = ['java', '-jar', self.jar, '--margs', filename]

        std_out, std_err = Query.run(cmd)

        if self.check_for_error(std_out, std_err):
            dicts = self.extract_margs_info(std_out)

            for dict in dicts:
                dict['filename'] = filename
                writer_obj.writerow(dict)
            self.write_status_to_overview(filename, "all ok")
            self.status_counts['success'] += 1
        else:
            self.write_status_to_overview(filename, self.determine_error_type(std_err))
            self.status_counts['errors'] += 1
            logging.error("Error in script execution. Output:\n" + std_err.replace("\r\n\tat", "\n"))

    def check_for_error(self, std_out, std_err):
        return std_err == ""



    def extract_margs_info(self, output):
        """
        Method that extracts information in allmargs mode.

        Possible output::

            Partition function is exp(-2.798187775203149E7)
            Building marginal query class for Att(X)
            Probability for class of queries Att(X) is exp(-422.95578300207853)
            Building marginal query class for Res(X)
            Probability for class of queries Res(X) is exp(-0.4700036309659481)
            [...]
            done
            [t_inference] {39140}

        Extracts the query strings and the corresponding probabilities, as well as the total time needed.

        :param output: output string from console forclift call
        :return: list of dictionaries containing the relevant information (see above)
        """
        dicts = []

        query_number = output.count("Probability for class of queries")

        lines = output.split("\n")

        start, end, t_inference = 0, 0, 0
        start_found = False
        for i, line in enumerate(lines):
            if "Building marginal query class" in line and not start_found:
                start = i
                start_found = True
            elif "Probability for class of queries" in line:
                end = i

            if "[t_inference]" in line:
                t_inference = line.split("{")[1].strip()[:-1]

        prob_lines = [line.strip() for line in lines[start:end+1]]

        for i in range(0, len(prob_lines), 2):
            query = prob_lines[i].split("Building marginal query class for ")[1].strip()
            prob = prob_lines[i+1].split("Probability for class of queries "+query+" is ")[1]

            dicts.insert(0, self.info_dict.copy())
            dicts[0]['t_inference'] = t_inference
            dicts[0]['query'] = query
            dicts[0]['P(query)'] = prob

        return dicts


class AlchemyEngine(BenchmarkEngine):
    def get_start_message(self):
        return "Running benchmark on framework: [Alchemy]"

    def __init__(self, directory, engine, maxSampleSteps=None):
        # Class config
        inference_engine = engine
        extension = "mln"
        log_suffix = "_alchemy.log"
        csv_suffix = '_alchemy_times.csv'
        ov_suffix = '_alchemy_overview.log'
        jar = './Alchemy_liftedinfer'
        self.max_sample_steps = maxSampleSteps

        csv_cols = ['filename', 'query', 'Z', 'probs', 'maxSteps', 'total time', 'millisecs']

        info_dict = {'filename': '', 'query': '', 'Z': -1,'probs':'', 'maxSteps':-1, 'total time': -1, 'millisecs':-1}

        super(AlchemyEngine, self).__init__(directory=directory,
                                            extension=extension,
                                            inference_engine=inference_engine,
                                            log_file_suffix=log_suffix,
                                            csv_file_suffix=csv_suffix,
                                            file_overview_suffix = ov_suffix,
                                            jar=jar,
                                            csv_columns=csv_cols,
                                            info_dict=info_dict)

    def extract_information(self, output):
        info_dict = self.info_dict.copy()

        if "Exact Z =" in output:
            # Extraction of exact inference values (engine: ptpe)
            """
            Output format like:
            [...]
            done writing dump files (mlndump.dat,symbolsdump.dat)
            Exact Z = Starting Exact Lifted Model Counting
            (Actual) 2.08035e+15
            [total time] : {0.06 secs}
            [millisecs] : {73}
            """
            lines = output[output.find("Exact Z ="):].split("\n")

            for i, line in enumerate(lines):
                if str(line).startswith("Exact Z ="):
                    info_dict['Z'] = lines[i+1].strip()
                    break

        elif "Z-curr =" in output:
            # Extraction of sampling-based (approximate) inference values (with engine lis)
            """
            Output format like:
            [...]
            iteration 975
            Z-curr =  (Actual) 9.59097e+10
            cumulative-Z =  (Actual) 1.31424e+14
            Updating Distributions...
            iteration 1000
            Z-curr =  (Actual) 9.59097e+10
            cumulative-Z =  (Actual) 1.34819e+14>
            [total time] : {0.44 secs}
            [millisecs] : {455}
            """
            lines = output[output.rfind("Z-curr ="):].split("\n")

            for i, line in enumerate(lines):
                if str(line).startswith("Z-curr ="):
                    info_dict['Z'] = line.replace("Z-curr =  (Actual)", "").strip()
                    break

        elif "LBG Sampling Process" in output:
            # Extraction of sampling-based (approximate) inference values (with engine lvg)
            """
            Output format like:
            [...]
            LBG Sampling Process::Sampling from cluster level 0,iter=950
            LBG Sampling Process::Sampling from cluster level 0,iter=975
            LBG Sampling Process::Sampling from cluster level 0,iter=1000
            Sampling Process exiting
            [total time] : {0.84 secs}
            [millisecs] : {866}
            """
            lines = output[output.rfind("Sampling Process exiting"):].split("\n")
            info_dict['Z']=-2 # no Z to extract available
        else: 
            logging.warning("'Exact Z =' or 'Z-curr =' not found in return string. \
            Skipping information extraction.")
            lines = []

        
        for line in [line for line in lines if line != "" and all(x in line for x in ["[", "]", "{", "}"])]:
            key = line[line.find("[") + 1:line.find("]")]
            value = line[line.find("{") + 1:line.find("}")]
            info_dict[key] = value

        info_dict['probs'] = self.extract_probs_from_file()
        info_dict['maxSteps'] = self.max_sample_steps

        return info_dict

    def check_for_error(self, std_out, std_err):
        return (std_out.find("error") == -1) and ("failed" not in std_err) 

    def get_error_message(self, std_out, std_err):
        return " > std_out:\n{}\n\n > std_err:\n{}".format(std_out, std_err)

    def execute_combined_query(self, comp_str, filename):
        """
        Method that executes a combined query string.
        :param comp_str: combined (composed) query string to be executed
        :return: Tuple (std_out, std_err) from cmd call.
        """
        # Only continue if OS is linux.
        if 'linux' not in sys.platform:
            sys.exit('Stopped benchmark. Alchemy benchmark is just supported on Linux not this OS.')

        cmd = [self.jar, "-" + self.inf_eng, 'true', '-q', comp_str, '-i', filename]

        return Query.run(cmd)

    def extract_probs_from_file(self):
        """
        Function for information extraction (probabilities) from the alchemy result.dat file
        :return: a string with all queries' probabilities in the form: '{Query(A):0.123;Query(B):0.1}'
        """

        with open("result.dat", 'r') as f:
            lines = [line.strip().replace(" ", ":") for line in f if line.strip() != ""]

        return "{" + ";".join(lines) +"}"

    def extract_queries(self, file_string, filename, engine):
        """
        Function that pulls queries out of a given file and cleans them up
        (removes comment-double-slashes ('//') and  empty strings).
        :param file_string: content of the file
        :param filename: filename
        :param engine: engine object (needed for passing on)
        :return: a list of query objects
        """

        # Reduce fileString to relevant lines
        query_lines = file_string.split("[Queries]")[1].split("\n")[3:]
        # Extract raw query strings from query lines
        query_strings = [query[3:].strip() for query in query_lines if query.startswith("//")]

        query_objects = [AlchemyQuery(query_string, filename, engine) for query_string in query_strings]

        return query_objects

    def cleanup(self):
        # Remove files if present:
        files = ['mlndump.dat', 'result.dat', 'symbolsdump.dat']
        for f in files:
            if os.path.exists(f):
                os.remove(f)


class GCFoveEngine(BenchmarkEngine):
    def __init__(self, directory, inference_engine):
        # Class config
        extension = "blog"

        inf_eng_str = "LVE" if (inference_engine == "fove.LiftedVarElim") else "VE"

        log_suffix = "_gcfove_"+inf_eng_str+".log"
        csv_suffix = '_gcfove_'+inf_eng_str+'_times.csv'
        ov_suffix = '_gcfove_'+inf_eng_str+'_overview.log'
        jar = 'gcfove.jar'

        csv_cols = ['filename', 'query', 'P(query)', 'time']
        info_dict = {'filename': '', 'query': '', 'P(query)':-1, 'time': -1}

        super(GCFoveEngine, self).__init__(directory=directory,
                                           extension=extension,
                                           inference_engine=inference_engine,
                                           log_file_suffix=log_suffix,
                                           csv_file_suffix=csv_suffix,
                                           file_overview_suffix = ov_suffix,
                                           jar=jar,
                                           csv_columns=csv_cols,
                                           info_dict=info_dict)

    def get_start_message(self):
        return "Running benchmark on framework: [GCFove]\nInference Engine: [{}]".format(self.inf_eng)

    def handle_combined_queries(self, queries, filename):
        # This function is called out of the handle_file() function, queries are already extracted (via extract_queries() )and 
        # forwarded as an argument.
        std_out, std_err = self.execute_combined_query("", filename)

        if std_err == "<Timeout>":
            logging.warning("Timeout in script execution.")
            self.write_status_to_overview(filename, "timeout")
            self.status_counts['timeouts'] += 1
            return []
        elif self.check_for_error(std_out, std_err):
            infos = self.extract_cq_information(queries, std_out)

            for dic in infos:
                dic["filename"] = filename
                
            # We need to return a list of info dicts
            self.write_status_to_overview(filename, "all ok")
            self.status_counts['success'] += 1
            return infos
        else:
            self.write_status_to_overview(filename, self.determine_error_type(std_err))
            logging.error("Error in script execution. Output:\n" + self.get_error_message(std_out, std_err).replace("\r\n\tat", "\n"))
            self.status_counts['errors'] += 1
            return[]
        
    def execute_combined_query(self, comp_string, filename):
        cmd = ['java', '-jar', self.jar, '-e', self.inf_eng, filename]
        if java_xmx:
            cmd.insert(2, '-Xmx16384M')

        # TODO: insert passthrough args? problem: Function is defined in Query class.
        #cmd = self.insert_passthrough_args(cmd, passThroughArgs)
        if passThroughArgs != "":
            raise Exception("Passing arguments through not yet implemented in GCFOVE combined query mode.")

        return Query.run(cmd)
        
    def check_for_error(self, std_out, std_err):
        return std_err == ""

    def extract_information(self, output):
        """
        Function that extracts relevant information from the GCFOVE output string.
        Returns a dictionary containing the relevant information
        """
        info_dict = self.info_dict.copy()

        # Extract substring between '**TIME**' and next occurence of linebreak ('\n')
        if "**TIME**" in output:
            time = output[output.find("**TIME**") + len("**TIME**"):
                          output.find("\n", output.find("**TIME**"))].strip()
        else:
            time = -1
            logging.warning("Output from jar execution not in standard format (missing '**TIME**').")

        info_dict['time'] = time

        probs = self.extract_probabilities(output)
        if len(probs) != 1:
            logging.warning("More than 1 probability output found when not in combined query mode for GCFOVE.")
            info_dict['P(query)']=-1
        else:
            info_dict['P(query)'] = probs[0][1]

        return info_dict

    def extract_cq_information(self, queries, std_out):
        """
        Extracts the information out of a combined query GCFOVE call and returns a list
        of info dicts.

        Takes into account whether the chosen engine is 'fove.LiftedVarElim' or 've.VarElimEngine'.
        The first returns 1 time per query, the latter 1 overall time.
        """
        # 1. extracting and storing times in times list.
        times = []
        if "**TIME**" in std_out:
            lines = std_out.splitlines()
            for line in lines:
                if line.startswith("**TIME**"):
                    val = line[len("**TIME**"):].strip()
                    times.append(val)
        else:
            times.append(-1)
            logging.warning("Output from jar execution not in standard format (missing '**TIME**').")

        probs = [tuple[1] for tuple in self.extract_probabilities(std_out)]

        # Write collected information to dicts.
        ret_dicts = []
        for i,q in enumerate(queries):
            tmp_dict = self.info_dict.copy()
            
            tmp_dict["query"] = q.queryString
            tmp_dict["P(query)"] = probs[i]
            tmp_dict["time"] = times[i if len(times)>1 else 0]

            ret_dicts.append(tmp_dict)
        return ret_dicts

    
    def handle_single_query_prob(self, split_output):
        """
        Handles the probability lines of a single query, e.g.
        `Distribution of values for Att(x1)`
        `0.9422090158242775      false`
        `0.05779098417572252     true`
        or
        `Distribution of values for TestQ(123)`
        `1                       false`
        or 
        `0.9997378024340892      false`
        `2.621975659107907E-4    true` // [note the scientific notation]


        Returns:
            A tuple of (Query, true_probability) of type (string, float)
        """
        lines = split_output.split("\n")
        if len(lines) not in [2,3]:
            logging.warning("Unexpected number of lines in probability extraction per query, expected 2 or 3, was {}: \n{}".format(len(lines), "\n".join(lines)))
            return (lines[0], float(-2))
            #raise Exception("Unexpected number of lines in probability extraction per query, expected 2 or 3, was {}.".format(len(lines)))
        query = lines[0]
        probs = lines[1:]

        found = False
        for prob in probs:
            if prob.find("true") != -1:
                #true_prob = re.findall(r"[-+]?\d*\.\d+|\d+|NaN", prob)[0]
                true_prob = re.findall(r"-?\d+(?:\.\d+)?(?:E-?\d+)|NaN|-?\d+(?:\.\d+)?", prob)[0]
                found = True
                break
        
        true_prob = true_prob if found else 0

        return (query, float(true_prob))

    def extract_probabilities(self, output):
        """
        Extracts the calculated probabilities from the generated console output.
        Returns:
            A list of (Query, Probability) tuples of type (string, float)
        """
        query_sep = "======== Query Results ========="
        if output.find(query_sep) == -1:
            logging.error("Query separator not found in console output.")
            print("Output: \n"+output)
            #raise Exception("Query separator not found.")
            return ("Q","error")
        else:
            txt = output.split(query_sep)[1]

            # Split in query parts
            split = txt.split("Distribution of values for ")[1:]

            l = [self.handle_single_query_prob(s.strip()) for s in split]
            
            return l
    
    def extract_queries(self, file_string, filename, engine):
        """
        Function that extracts a list of queries from a BLOG-file.
        Returns query objects (not strings).
        """
        lines = [line.strip() for line in file_string.split("\n") if "query" in line]

        queries = [GCFoveQuery(line, filename, engine) for line in lines]
        return queries

    def cleanup(self):
        # no cleanup necessary
        pass

class JTEngine(GCFoveEngine):
    def __init__(self, directory, inference_engine):
        extension = "blog"

        inf_eng_str_dict = {"fove.LiftedVarElim": "LVE",
                            "ve.VarElimEngine": "VE",
                            "fojt.LiftedJTEngine": "LJT",
                            "jt.JTEngine": "JT"}
        
        inf_eng_str = inf_eng_str_dict[inference_engine]
        self.inf_eng_str = inf_eng_str

        self.base_engine = "JT" if inf_eng_str in ['LJT', 'JT'] else "VE"
        
        log_suffix = "_JT_" + inf_eng_str + ".log"

        csv_suffix = '_JT_'+inf_eng_str+'_times.csv'
        ov_suffix = '_JT_'+inf_eng_str+'_overview.log'
        jar = 'fojt.jar'

        csv_cols = ['filename', 'query', 'P(query)', '|gr|', '|G|', '|E|', '|Q|', 'width', 'size', 'mem', 'VE_ops', 't_0', 't_1', 't_2', 't_queries', 'time']
        info_dict = {'filename': '', 'query': '', 'P(query)':-1, 'time': -1, '|gr|':-1, '|G|':-1, '|E|':-1, '|Q|':-1, 'width':-1, 'size':-1,
                    'mem': -1, 'VE_ops':'', 't_0': -1, 't_1': -1, 't_2': -1, 't_queries': '', 'time': -1}

        if inf_eng_str in ['VE', 'LVE']:
            del_cols = ['size', 'width', 't_0', 't_1', 't_2']
            for c in del_cols:
                info_dict[c] = -2

            print(">>>> ", info_dict)


        #super(JTEngine, self).__init__(directory=directory,
        super(GCFoveEngine, self).__init__(directory=directory,
                                           extension=extension,
                                           inference_engine=inference_engine,
                                           log_file_suffix=log_suffix,
                                           csv_file_suffix=csv_suffix,
                                           file_overview_suffix = ov_suffix,
                                           jar=jar,
                                           csv_columns=csv_cols,
                                           info_dict=info_dict)

    def get_start_message(self):
        return "Running benchmark on framework: [JT]\nInference Engine: [{}]".format(self.inf_eng)

    def check_for_error(self, std_out, std_err):
        """
        Checks for errors in script execution. Returns TRUE if no error occured (all is ok).

        Namely, the following conditions are met:
        * `std_err` is empty
        * no "mem error" in `std_out`
        """

        return super(JTEngine, self).check_for_error(std_out, std_err) and ("mem error" not in std_out)

    def get_error_message(self, std_out, std_err):
        return " > std_out:\n{}\n\n > std_err:\n{}".format(std_out, std_err)

    def extract_cq_information(self, queries, std_out):
        """
        Extracts the information in the combined query modes.

        Returns a list of info dicts (1 per query). 
        """
        prob_output, info_output, time_output = self.split_output_prob_info_time(std_out)
        probs = self.extract_probabilities(prob_output)
        #info_dict['query'] = " ,".join([q.queryString for q in queries])
        
        time_dict = self.dict_from_time_output(time_output)
        time_dict['time'] = time_dict['t_total']

        ret = []
        for i, q in enumerate(queries):
            loc_dict = self.info_dict.copy()
            self.extract_from_info_output(loc_dict, info_output)
            loc_dict['query'] = q.queryString
            loc_dict['P(query)'] = probs[i][1]
            time_copy_keys = ['time', 't_0', 't_1', 't_2', 'VE_ops', 'mem'] if self.base_engine == "JT" else ['time', 'VE_ops', 'mem']
            for key in time_copy_keys:
                loc_dict[key] = time_dict[key]
            t_index = i+2 if self.base_engine == "JT" else i
            if 't_{}'.format(t_index) in time_dict:
                loc_dict['t_queries'] = time_dict['t_{}'.format(t_index)]

            ret.append(loc_dict)

        return ret 

    def extract_information(self, output):
        """
        Function that extracts relevant information from the JT output string.
        Returns a dictionary containing the relevant information
        """
        info_dict = self.info_dict.copy()

        prob_output, info_output, time_output = self.split_output_prob_info_time(output)
        probs = self.extract_probabilities(prob_output)
        if len(probs) != 1:
            logging.warning("More than 1 probability output found when not in combined query mode for JT.")
            info_dict['P(query)']=-1
        else:
            info_dict['P(query)'] = probs[0][1]
        
        self.extract_from_info_output(info_dict, info_output)
        self.extract_from_time_output(info_dict, time_output)       

        return info_dict

    def split_output_prob_info_time(self, output):
        """
        Target: Split output into three parts:
        probabilities, info, times_mem

        Format is:
        ...
        [Probabilities]
        ...
        engine  xyz
        name    abc
        ...
        Split times
        t_0     1234 ns     1234 ms
        t_1     1234 ns     1234 ms
        ...
        t_total 1234 ns     1234 ms
        mem     164 B       16 kB
        VE ops  tot=SO,CC,CE,Pr,Ab,Sp
        VE ops  7=6,0,0,0,0,1 
        """
        prob_start_marker = "======== Query Results ========="
        prob_start = output.index(prob_start_marker)
    
        prob_end_marker = "engine\t"
        prob_end = output.index(prob_end_marker)

        info_end_marker = "Split times\n"
        info_end = output.index(info_end_marker)
        
        return output[prob_start:prob_end], output[prob_end:info_end], output[info_end+len(info_end_marker):]

    def extract_from_info_output(self,info_dict, info_output):
        """
        Extracts information from the info output and writes extracted info to info_dict.

        Format of info_output:
        ```
        engine  fojt.LiftedJTEngine
        name    temp_multi_query_model
        |G|     4
        |gr|    31
        |Q|     1
        |E|     0
        size    3
        width   2
        ```
        """
        ignore_keys = ['engine', 'name']

        lines = info_output.split("\n")
        for line in lines:
            if line != "":
                key = line.split("\t")[0]
                val = line.split("\t")[1]
                if key not in ignore_keys:
                    info_dict[key] = val

    def extract_from_time_output(self, info_dict, time_output):
        """
        Extracts information from the time_output and writes extracted info to info_dict.

        Format of time_output:
        ```
        t_0     171026000 ns    171 ms
        t_1     20700 ns        0 ms
        t_2     13375200 ns     13 ms
        t_3     4157000 ns      4 ms
        t_total 256901000 ns    256 ms
        mem     1639968 B       1601 kB
        VE ops  tot=SO,CC,CE,Pr,Ab,Sp
        VE ops  7=6,0,0,0,0,1
        ```
        """
        lines = time_output.split("\n")
        t_queries = []
        for line in lines:
            if line != "":
                val_ind = 1 if line.count("\t") == 1 else 2
                key = line.strip().split("\t")[0].replace(" ","_")
                val = line.strip().split("\t")[val_ind]
                
                # filter "unit" from val (temp & mem)
                if key.startswith("t_") or key == "mem":
                    val = [int(s) for s in val.split() if s.isdigit()][0]
                
                if key == "t_total":
                    info_dict['time'] = val
                elif key.startswith("t_") and int(key.split("_")[1]) >= 3:
                    t_queries.append(val)
                else:     
                    info_dict[key] = val

        if self.base_engine == "VE":
            info_dict['t_queries'] = "["+", ".join([str(x) for x in t_queries])+"]"
        elif self.base_engine == "JT":
            info_dict['t_queries'] = info_dict['t_0']
            info_dict['t_0'] = -2


    def dict_from_time_output(self, time_output):
        """
        Parses the time_output to a dictionary.

        Returns the dictionary with the keys from the time_output.
        """
        lines = time_output.split("\n")
        d = {}
        for line in lines:
            if line != "":
                val_ind = 1 if line.count("\t") == 1 else 2
                key = line.strip().split("\t")[0].replace(" ","_")
                val = line.strip().split("\t")[val_ind]
                
                # filter "unit" from val (temp & mem)
                if key.startswith("t_") or key == "mem":
                    val = [int(s) for s in val.split() if s.isdigit()][0]
                d[key] = val
        return d


class BLOGEngine(BenchmarkEngine):
    def __init__(self, directory, inference_engine):
        # Class config
        extension = "blog"

        inf_eng_str = "SamplingEngine" 

        if inference_engine != "SamplingEngine":
            logging.info("Using default inference engine: "+inf_eng_str)

        log_suffix = "_blog_"+inf_eng_str+".log"
        csv_suffix = '_blog_'+inf_eng_str+'_times.csv'
        ov_suffix = '_blog_'+inf_eng_str+'_overview.log'
        jar = 'blog'

        csv_cols = ['filename', 'query', 'P(query)', 'time']
        info_dict = {'filename': '', 'query': '', 'P(query)':-1, 'time': -1}

        super(BLOGEngine, self).__init__(directory=directory,
                                           extension=extension,
                                           inference_engine=inference_engine,
                                           log_file_suffix=log_suffix,
                                           csv_file_suffix=csv_suffix,
                                           file_overview_suffix = ov_suffix,
                                           jar=jar,
                                           csv_columns=csv_cols,
                                           info_dict=info_dict)

    def get_start_message(self):
        return "Running benchmark on framework: [BLOG]\nInference Engine: [{}]".format(self.inf_eng)
    
    def check_for_error(self, std_out, std_err):
        return std_err == ""

    def handle_single_queries(self, filename, writer_obj):
        # TODO: Single query mode for BLOG? No 'query' argument -> 1 file per query needed
        logging.error("For now, BLOG framework just supports combined query mode (`-cq`). Exiting...")
        sys.exit()

    def execute_combined_query(self, comp_string, filename):
        blog_dir = 'BLOGEngine'
        lib_dir = 'lib'

        libs = ["blog.blog-0.10.alpha1.jar", "java-cup-11b.jar;zmq.jar", "org.scala-lang.scala-library-2.10.6.jar",
            "gov.nist.math.jama-1.0.3.jar", "com.google.code.gson.gson-2.2.4.jar", "org.apache.commons.commons-math3-3.0.jar",
                "de.jflex.jflex-1.6.0.jar", "org.apache.ant.ant-1.7.0.jar", "org.apache.ant.ant-launcher-1.7.0.jar", 
                    "com.github.tototoshi.scala-csv_2.10-1.1.1.jar"]
        
        lib_sep = ";" # TODO if linux -> ':'!
        lib_string = lib_sep.join([os.path.join(blog_dir, lib_dir, lib) for lib in libs])
        main_class = "blog.Main"

        cmd = ['java', '-cp', lib_string, main_class, filename]

        # print "cmd: \n" + " ".join(cmd)

        return Query.run(cmd)

    def extract_information(self, output):
        info_dict = self.info_dict.copy()

        # Extract substring between '' and next occurence of linebreak ('\n')
        time_tag = "Total elapsed time:"
        if time_tag in output:
            time = output[output.find(time_tag) + len(time_tag):
                          output.find("\n", output.find(time_tag))].strip()
        else:
            time = -1
            logging.warning("Output from jar execution not in standard format (missing '{}').".format(time_tag))

        info_dict['time'] = time

        # TODO: Handle Multiple Query Probabilities? Right now, just the first one is extracted.
        probs = self.extract_probabilities(output)
        info_dict['P(query)'] = probs[0][1]

        return info_dict

    def handle_single_query_prob(self, split_output):
        """
        Handles the probability lines of a single query, e.g.
        `Distribution of values for Att(x1)`
        `0.9422090158242775      false`
        `0.05779098417572252     true`
        or
        `Distribution of values for TestQ(123)`
        `1                       false`

        Returns:
            A tuple of (Query, true_probability) of type (string, float)
        """
        lines = split_output.split("\n")
        if len(lines) not in [2,3]:
            logging.warning("Unexpected number of lines in probability extraction per query, expected 2 or 3, was {}: \n{}".format(len(lines), "\n".join(lines)))
            return (lines[0], float(-2))
            #raise Exception("Unexpected number of lines in probability extraction per query, expected 2 or 3, was {}.".format(len(lines)))
        query = lines[0]
        probs = lines[1:]

        found = False
        for prob in probs:
            if prob.find("true") != -1:
                true_prob = re.findall(r"-?\d+(?:\.\d+)?(?:E-?\d+)|NaN|-?\d+(?:\.\d+)?", prob)[0]
                found = True
                break
        
        true_prob = true_prob if found else 0

        return (query, float(true_prob))

    def extract_probabilities(self, output):
        """
        Extracts the calculated probabilities from the generated console output.
        Returns:
            A list of (Query, Probability) tuples of type (string, float)
        """
        query_sep = "======== Query Results ========="
        if output.find(query_sep) == -1:
            logging.error("Query separator not found.")
            logging.error("Output: \n"+output)
            # raise Exception("Query separator not found.")
        txt = output.split(query_sep)[1]
        txt = txt.split("Total elapsed time:")[0]

        # Split in query parts
        split = txt.split("Distribution of values for ")[1:]

        l = [self.handle_single_query_prob(s.strip()) for s in split]
        
        return l
    
    def extract_queries(self, file_string, filename, engine):
        """
        Function that extracts a list of queries from a BLOG-file.
        Returns query objects (not strings).
        """
        lines = [line.strip() for line in file_string.split("\n") if "query" in line]

        queries = [Query(line, filename, engine) for line in lines]
        return queries
    
    def cleanup(self):
        # no cleanup necessary
        pass
        
    
class Query(object):
    def __init__(self, query_string, filename, benchmark_engine, pt_arg_index=3):
        self.queryString = query_string
        self.filename = filename
        self.benchmark_engine = benchmark_engine

        # Index in command array where pass through arguments should be inserted
        # (default is 3, 'java -jar file.jar args' structure)
        self.pt_arg_index = pt_arg_index

    def __repr__(self):
        self.__str__()

    
    def insert_passthrough_args(self, cmd_array, passthrough_args):
        """
        Inserts specified pasthrough arguments (if any are given) at the correct 
        position in the command array.
        """
        if "'" in passthrough_args:
            passthrough_args = passthrough_args.replace("'", "")
        elif '"' in passthrough_args:
            passthrough_args = passthrough_args.replace('"', '')

        if passthrough_args != "":
            # split passthrough args at empty spaces and flat-insert that list at the pt_arg_index
            cmd_array[self.pt_arg_index:self.pt_arg_index] = passthrough_args.split(" ")
        return cmd_array


    @abc.abstractmethod
    def execute(self):
        """
        Executes the query. Implemented in subclasses depending on query structure.
        """

    @staticmethod
    def run(cmd):
        """
        Method that runs a given command (as a list of strings) in the console.
        Kills the process after timeout_secs seconds.
        source: https://stackoverflow.com/a/10012262/5604353
        """
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        # timeout flag given as list (so that call by reference on timeout[0] is used)
        timeout = [False]
        timer = Timer(timeout_secs, kill_proc, [proc, timeout])
        try:
            timer.start()
            stdout, stderr = proc.communicate()
            stdout = stdout.decode('UTF-8')
            stderr = stderr.decode('UTF-8')
        finally:
            timer.cancel()

        if verbose:
            print("> cmd line call: '"+" ".join(cmd))+"'"
            print("\n> std_out:\n"+stdout)
            print("\n> std_err:\n"+stderr)
        
        if timeout[0]:
            return "", "<Timeout>"
        else:
            return stdout, stderr

    def __str__(self):
        """
        Implemented for query logging info printing.
        """
        return self.queryString


def kill_proc(proc, timeout):
    """
    Process kill wrapper that also writes a warning to the log.
    """
    proc.kill()
    logging.warning("<<< Process timeout (after {} seconds) >>>".format(str(timeout_secs)))
    timeout[0] = True


class ForcliftQuery(Query):
    def __init__(self, query_string, filename, benchmark_engine):
        super(ForcliftQuery, self).__init__(query_string, filename, benchmark_engine)

    def execute(self):
        cmd = ['java', '-jar', self.benchmark_engine.jar, '-q', self.queryString, self.filename]
        if java_xmx:
            cmd.insert(2, '-Xmx16384M')
        
        cmd = self.insert_passthrough_args(cmd, passThroughArgs)
        
        return self.run(cmd)


class AlchemyQuery(Query):
    def __init__(self, query_string, filename, benchmark_engine):
        pt_arg_index = 1
        super(AlchemyQuery, self).__init__(query_string, filename, benchmark_engine, pt_arg_index)

    def execute(self):
        # Only continue if OS is linux.
        if 'linux' not in sys.platform:
            sys.exit('Stopped benchmark. Alchemy benchmark is just supported on Linux not this OS.')

        cmd = [self.benchmark_engine.jar, "-"+self.benchmark_engine.inf_eng, 'true', '-q', self.queryString, '-i', self.filename]
        # Look for *.db evidence files
        db = self.find_evidence_db()
        if db != "":
            cmd.insert(1, db)
            cmd.insert(1, '-e')

        if engine.max_sample_steps != None:
            cmd.insert(1, str(engine.max_sample_steps))
            cmd.insert(1, '-maxSteps')

        cmd = self.insert_passthrough_args(cmd, passThroughArgs)

        # TODO: Check for output that indicates that packages are missing.
        # Install packages: libc6-i386, lib32stdc++6

        logging.info("running cmd with: "+" ".join(cmd))
        return self.run(cmd)

    def find_evidence_db(self):
        db_path = self.filename[:self.filename.rfind('.')] + ".db"
        if os.path.exists(db_path):
            logging.info("    Including evidence file '%s' for model file '%s' in query", db_path, self.filename)
            return db_path
        else:
            return ""


class GCFoveQuery(Query):
    def __init__(self, query_string, filename, benchmark_engine):
        super(GCFoveQuery, self).__init__(query_string, filename, benchmark_engine)

    def execute(self):
        query_count = self.count_queries(self.filename)
        if query_count > 1:
            # Do multiple query handling
            temp_file = self.create_single_query_modelfile(self.queryString, self.filename)
            logging.info("    More than 1 query found. Creating temporary model file (with single query): %s", temp_file)
            std_out, std_err = self.inference(temp_file, self.benchmark_engine)
            os.remove(temp_file)
            return std_out, std_err
        else:
            return self.inference(self.filename, self.benchmark_engine)

    @staticmethod
    def count_queries(file_path):
        """
        Returns the number of queries present in a BLOG file
        """
        with open(file_path, 'r') as f:
            model_string = f.read()
            return model_string.lower().count('query')

    @staticmethod
    def create_single_query_modelfile(query, filename):
        """
        Method that (temporarily) creates a BLOG-file with one single query.
        Returns the filename of the temp file.
        """
        with open(filename, 'r') as f:
            lines = f.read().split("\n")

        # remove query lines:
        lines_wo_queries = [line for line in lines if 'query' not in line.lower()]

        # append single query line
        final_lines = lines_wo_queries + [query]

        temp_filename = "temp_" + os.path.basename(filename)
        with open(temp_filename, "w+") as f:
            f.write("\n".join(final_lines))

        return temp_filename

    def inference(self, filename, engine):
        """
        Method that actually executes the inference engine and returns its output
        """
        cmd = ['java', '-jar', engine.jar, '-e', engine.inf_eng, filename]
        if java_xmx:
            cmd.insert(2, '-Xmx16384M')

        cmd = self.insert_passthrough_args(cmd, passThroughArgs)
        return self.run(cmd)


class ErrorMessageScanner(object):
    """
    TODO: Check if output is:
    Unable to access jarfile {}
    -> sys.exit()
    """

    """
    TODO: Check if output is "file not found"
    -> sys.exit() and suggest installation of packages 
    """

if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument('--framework', '-f', required=True, choices=['forclift', 'gcfove', 'jt','alchemy', 'blog'],
                        help="framework to be benchmarked")

    parser.add_argument('--verbose', '-v', action='store_true', help="prints jar output and extracted information to console (not to log)")

    parser.add_argument('--engine', '-e', help='inference engine to be chosen (if multiple available)',
                        choices=['fove.LiftedVarElim', 've.VarElimEngine', 'fojt.LiftedJTEngine', 'jt.JTEngine', 'ptpe', 'lis', 'lvg'])

    parser.add_argument('--combinequeries', '-cq', help="performs queries all at once (if possible), "
                                                        "instead of one by one", action='store_true', default=False)

    parser.add_argument('--maxSampleSteps', '-ms', help="maximum number of MCMC sampling steps (for Alchemy sampling algos).",
                        type=int, default=None)

    parser.add_argument('--passThroughArgs', '-pt', help='argument string that is simply passed through to the engine. Enquoted with double quotes'
                         ' and presented with equals sign between -pt and string -> -pt="ARGSTRING"', default= '')

    parser.add_argument('--timeoutSkip' , '-ts', help ="DEactivates Timeout exclude mode: if file_a.xyz leads to timeout don't even start on bigger file_b.xyz with b > a",
    					action='store_false')

    parser.add_argument('--timeout', '-t', type=int, help='timeout (seconds) after which processes will be killed',
                        default=300)

    parser.add_argument('--java_xmx', '-x', help="adds the java -Xmx16384M argument to the console calls",
                        action='store_true')

    parser.add_argument('directory', help="relative path to model file directory", nargs='?', default=".")

    args = parser.parse_args()

    timeout_secs = args.timeout
    combine_queries = args.combinequeries
    verbose = args.verbose
    java_xmx = args.java_xmx
    maxSampleSteps = args.maxSampleSteps
    passThroughArgs = args.passThroughArgs
    timeout_skip = args.timeoutSkip


    if args.framework == 'forclift':
        engine = ForcliftEngine(args.directory)
        framework_string = 'Forclift'
    elif args.framework == 'gcfove':
        eng = "fove.LiftedVarElim" if args.engine is None else args.engine
        engine = GCFoveEngine(args.directory, eng)
        framework_string = 'GCFove'
    elif args.framework == 'jt':
        eng = "fojt.LiftedJTEngine" if args.engine is None else args.engine
        engine = JTEngine(args.directory, eng)
        framework_string = "JT"
    elif args.framework == 'alchemy':
        eng = "ptpe" if args.engine is None else args.engine
        engine = AlchemyEngine(args.directory, eng, maxSampleSteps)
        framework_string = 'Alchemy'
    elif args.framework == 'blog':
        engine = BLOGEngine(args.directory, args.engine)
        framework_string = 'BLOG'

    engine.main_wrapper()