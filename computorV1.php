<?php
/**
 * Created by PhpStorm.
 * User: sandruse
 * Date: 7/16/18
 * Time: 6:36 PM
 */

class Validator{

    public $error;

    public function validateEquation($equation): bool {
        $wrong_order = 0;
        $equals_sign_last = false;
        $have_equals_sign = false;
        $have_x = false;
        $sings = 0;    // [ - || + || =]
        $no_sings = 0; // [ float || int || like 3X^2]

        $parts = explode(' ', trim($equation));

        $pattern = '/^[-+]?[0-9]*\.?[0-9]*[x]?(\^\d+)?$/';

        foreach ($parts as $part){
            if($wrong_order > 1 || $wrong_order < 0){
                $this->error = 'Bad order of symbols in your equation.'."\n";
                return false;
            }
            if (preg_match('/^[+|\-|=]{1}$/', $part)){
                $wrong_order--;
                $sings++;
                if ($equals_sign_last){
                    $equals_sign_last = false;
                }
                if (strstr($part,'=')){
                    $equals_sign_last = true;
                    $have_equals_sign = true;
                }
            }
            else if (preg_match($pattern, $part) ){
                $wrong_order++;
                $no_sings++;
                if ($equals_sign_last){
                    $equals_sign_last = false;
                }
                if (strstr($part,'x')){
                    $have_x = true;
                }
            }
            else {
                $this->error = 'Bad syntax. You have more than one space one after another , non-numeric parts or some another mistake in your equation near '.$part."\n";
                return false;
            }
        }
        if ($sings == ($no_sings - 1) && $have_x && $have_equals_sign){
            return true;
        }
        else {
            $this->error = ($have_x)
                ? ($have_equals_sign)
                    ? ($equals_sign_last)
                        ?'There is no symbol after symbol ='."\n"
                        :'Bad syntax. You have wrong number of signs in your equation'."\n"
                    : 'There is no symbol = in the equation'."\n"
                : 'There is no symbol X in the equation'."\n";
            return false;
        }
    }

    public function prepareString($string): string {
       return strtolower(str_replace([' * ','* ','* ', '*'], '', $string));
    }
}


class Part{

    public $sign;
    public $is_x;
    public $index;
    public $degree;

    public function __construct($part, $is_x = false, $sign = false)
    {
        $this->sign = $sign;
        $this->is_x = $is_x;

        if ($is_x)
        {
            $this->split_x($part);
        }
        else {
            $this->split_number($part);
        }
    }

    public function split_x($part){

        $split = explode('x', trim($part));

        $this->index = ($split[0] != '') ? (strstr($split[0],'.')) ? floatval($split[0]) : intval($split[0]) : 1;

        if ($this->index === 0)
        {
            $this->is_x = false;
            $this->degree = false;
        }
        else {
            $this->degree = (strstr($split[1], '^')) ? intval(trim($split[1],'^ ')) : 1;

            if ($this->degree === 0){
                $this->degree = false;
                $this->is_x = false;
            }
        }
    }

    public function split_number($part){

        if (strstr($part, '^')){

            $split = explode('^', trim($part));

            if ($split[0] == '') {
                echo 'Bad syntax. You have more than one space one after another , non-numeric parts or some another mistake in your equation near '.$part."\n";
            }
            else {

                $this->index = (strstr($split[0],'.')) ? floatval($split[0]) : intval($split[0]);

                $this->degree = intval($split[1]);

                $this->index = ($this->degree === 0) ? 1 : $this->to_degree();

            }
        }
        else {
            $this->degree = false;
            $this->index = (strstr($part,'.')) ? floatval($part) : intval($part);
        }
    }

    public function to_degree(){
        $index = $this->index;
        for ($x = $this->degree; $x > 1; $x--){
            $index = $index * $this->index;
        }
        $this->degree = false;
        return $index;
    }
}

class Equation{

    public $basic_equation;
    public $to_zero_equation;
    public $all_left;
    public $final_equation;

    public $parts = [];
    public $signs = [];

    public $validator;

    public $x_parts = [];
    public $numeric;
    public $is_quadratic;

    public function __construct($equation)
    {
        $this->validator = new Validator();

        if ($this->validator->validateEquation($equation)){
            $this->basic_equation = $equation;
            $this->right_to_zero($equation);
            $this->split_parts();
            $this->addSigns();
            $this->unite_same_part();
            ($this->is_quadratic)
                ? $this->solve_quadratic()
                : $this->solve_linear();

        } else{
            echo $this->validator->error;
            die();
        }
    }

    public function right_to_zero($equation){

        $parts = explode('=', trim($equation));

        $left_part = trim($parts[0]);

        $minus_string = str_replace(' - ', ' m ',trim($parts[1]));
        $plus_string = str_replace(' + ', ' p ',$minus_string);

        $minus_string_new = str_replace(' p ', ' - ',$plus_string);
        $plus_string_new = str_replace(' m ', ' + ',$minus_string_new);

        $this->to_zero_equation = $left_part. ' - ' . $plus_string_new. ' = 0';
        $this->all_left = $left_part. ' - ' . $plus_string_new;
    }

    public function split_parts(){
        $parts = explode(' ', $this->all_left);
        foreach ($parts as $part){
            if (strstr($part,'x')){
                $this->parts[] = new Part($part, true);
            }
            elseif ( preg_match('/^[+|\-|=]{1}$/', $part)){
                $this->signs[] = $part;
            }
            else {
                $this->parts[] = new Part($part);
            }
        }
    }

    public function addSigns(){
        foreach ($this->parts as $key => $part){
            $part->sign = ($key == 0) ? '+' : $this->signs[$key - 1];
        }
    }

    public function unite_same_part(){

        $x_array = [];
        $new_parts = [];
        $numeric = '0';
        foreach ($this->parts as $part){

            if ($part->is_x){
                $this->x_parts[] = $part;
            }
            else {
                $numeric = $numeric.' '.$part->sign.' '.$part->index;
            }
        }

        $this->numeric = eval('return '.$numeric.';');

        foreach ($this->x_parts as $part){
            $x_array[$part->degree][] = $part;
        }

        foreach ($x_array as $part_array){
            $part = $this->create_new_part($part_array);
            if ($part){
                $new_parts[] = $part;
            }
        }

        $this->parts = $new_parts;

        $last_one = ''.$this->numeric;

        foreach ($this->parts as $part){
            $last_one = $last_one.' '.$part->sign.' '.$part->index.'x^'.$part->degree;
        }

        $this->final_equation = $last_one.' = 0';

        foreach ($this->parts as $part){
            if ($part->degree > 2){
                echo 'Basic form:'.$this->basic_equation."\n";
                echo 'Reduced form: '.$this->to_zero_equation."\n";
                echo 'Final form: '.$this->final_equation."\n";
                echo 'Polynomial degree is '.$part->degree.' and it greater than 2. Sorry I can\'t solve this polynomial equation'."\n";
                die();
            }
        }
        echo 'Basic form:'.$this->basic_equation."\n";
        echo 'Reduced form: '.$this->to_zero_equation."\n";
        echo 'Final form: '.$this->final_equation."\n";
    }

    public function create_new_part(array $parts){
        $sign = null;
        $index = '0';
        $degree = $parts[0]->degree;
        foreach ($parts as $part){
            $index = $index.' '.$part->sign.' '.$part->index;
        }
        $index = eval('return '.$index.';');
        $sign = ($index >= 0) ? '+' : '-';
        if ($index === 0){
            return null;
        }
        else {
            if ($degree == 2){
                $this->is_quadratic = true;
            }
            return new Part($index.'x^'.$degree, true, $sign);
        }
    }

    public function solve_linear(){
        $new_numeric = $this->numeric * -1;
        echo 'Solve : '."\n";
        echo "\t".$this->parts[0]->index.'x = '.$new_numeric."\n";
        $x = $new_numeric /  $this->parts[0]->index;
        echo "\t".'x = '.$x."\n";
    }

    public function sqrt($value)
    {
        if ($value == 0 || $value == 1)
            return ($value);
        $res = $value;
        do {
            $diff = $res;
            $res = 0.5 * ($res + $value / $res);
        } while ($res != $diff);
        return ($res);
    }

    public function solve_quadratic(){
        $a = 0;
        $b = 0;
        $c = $this->numeric;
        foreach ($this->parts as $part){
            if ($part->degree == 2){
                $a = $part->index;
            }
            else {
                $b = $part->index;
            }
        }

        $D = $b * $b - (4 * $a * $c);

        if ($D > 0){
            echo "Discriminant is strictly positive, the two solutions are:"."\n";
            $x1 = (($b * -1) + $this->sqrt($D)) / (2 * $a);
            $x2 = (($b * -1) - $this->sqrt($D)) / (2 * $a);
            echo 'x1 = '.$x1."\n";
            echo 'x2 = '.$x2."\n";
        }
        elseif ($D = 0){
            $x = ($b / (2 * $a)) * -1;
        }
        else {
            echo 'Discriminant is strictly negative.There are no real roots for this equation'."\n";
        }
    }

}

function main($argc, $argv){
    if ($argc == 2){
        $string = (new Validator())->prepareString($argv[1]);

        $string = (new Validator())->prepareString($string);

        $equation = new Equation($string);

    } else {
        echo "\033[31mBad numbers of arguments.\n\033[0m";
    }
}

main($argc, $argv);