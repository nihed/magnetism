import logging

class StdoutLogger:
    """Class that can be assigned to sys.stdout to redirect stdout to the debug framework"""
    
    def __init__(self):
        self.__logger = logging.getLogger("bigboard.Stdout")
        self.__buffer = ""
    
    def write(self, str):
        self.__buffer += str
        while True:
            index = self.__buffer.find("\n")
            if index < 0:
                break
            self.__logger.debug(self.__buffer[0:index])
            self.__buffer = self.__buffer[index + 1:]
