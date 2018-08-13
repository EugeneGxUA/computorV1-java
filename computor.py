#!/usr/bin/env python
import sys
import re
from copy import copy
from collections import OrderedDict


def solve_equation(a, c, b=0.0):
    try:
        if b == 0.0:
            result = c * -1 / a
            return result
        else:
            descrimenant = (b ** 2 - 4 * a * c)

            if descrimenant == 0.0:
                return str(round((b * -1) / (2 * a), 4))
            descrimenant_root = descrimenant ** 0.5

            if isinstance(descrimenant_root, complex):
                result = (
                    '({c.real:.2f} + {c.imag:.2f}i)'.format(c=((b * -1 - descrimenant_root) / (2 * a))),
                    '({c.real:.2f} + {c.imag:.2f}i)'.format(c=((b * -1 + descrimenant_root) / (2 * a)))
                )
            else:
                result = (
                        str(round((b * -1 - descrimenant_root) / (2 * a), 4)),
                        str(round((b * -1 + descrimenant_root) / (2 * a), 4)),
                    )
            return result
    except ZeroDivisionError:
        print('There is no solution for equation')
        exit(0)


def validate_symbols(raw_equation: str, symbol='all'):
    is_invalid = False
    message = ''
    if symbol == 'all':
        symbols = re.findall(r'[\+\-]+=+|[\-\+\^]{2,}', raw_equation.replace(' ', ''))
        is_invalid = len(symbols) > 0
        message = 'Invalid string'
    elif symbol == '=':
        is_invalid = len(re.findall(r'=', equation)) != 1
        message = 'Invalid string: too many equation marks'
    if is_invalid:
        print(message)
        exit(0)


def get_multipliers_and_powers(equation_part: str):
    validate_symbols(equation_part, symbol='all')
    validate_symbols(equation)
    multipliers = [float(matching) for matching in re.findall(r'-?\d+\.?\d*(?=\*[xX]\^)', equation_part)]
    powers = [matching for matching in re.findall(r'(?<=[Xx]\^)\d+', equation_part)]

    if len(multipliers) != len(powers) or not powers or not multipliers:
        print('Invalid string')
        exit(0)
    return dict(zip(powers, multipliers))


def extract_zero_multipliers(equation_values: dict):
    for power, multiplier in copy(equation_values).items():
        if multiplier == 0.0:
            equation_values.pop(power)
    return equation_values


def reduce_form(left_part_values: dict, right_part_values: dict):
    """
    :param left_part_values:
    :param right_part_values:
    :return: OrderedDict of key(powers), values(multipliers) sorted by powers
    """
    left_part_len = len(left_part_values)
    right_part_len = len(right_part_values)

    base_part = left_part_values
    secondary_part = right_part_values
    if left_part_len < right_part_len:
        base_part = right_part_values
        secondary_part = left_part_values

    resulted_values = dict()
    for power, multiplier in base_part.items():
        try:
            secondary_multiplier = secondary_part.get(power)
            # if not secondary_multiplier:
            if secondary_multiplier is None:
                raise KeyError
        except KeyError:
            resulted_values[power] = multiplier
        except SystemError:
            print('powers not equal')
        else:
            resulted_values[power] = multiplier - secondary_multiplier

    resulted_values = extract_zero_multipliers(resulted_values)
    resulted_values = OrderedDict(sorted(resulted_values.items(), key=lambda elem: elem[0]))
    return resulted_values


def stringify_reduced_equation(equation_values: OrderedDict):
    if not equation_values:
        return '0 = 0'
    stringified_equation = ' + '.join('{} * X^{}'.format(multiplier, power) for power, multiplier in equation_values.items() if multiplier != 0.0)
    stringified_equation = stringified_equation.replace('+ -', '-').replace('-', '- ') + ' = 0'
    return stringified_equation


def get_coeficients(equation: str):

    left_part_values = dict.fromkeys(['0', '1', '2'], 0.0)
    right_part_values = dict.fromkeys(['0', '1', '2'], 0.0)

    equation = equation.replace(' ', '')
    left_equation_part, right_equation_part = equation.split('=')
    left_part_values.update(get_multipliers_and_powers(left_equation_part))
    right_part_values.update(get_multipliers_and_powers(right_equation_part))
    return reduce_form(left_part_values, right_part_values)


if __name__ == '__main__':
    message = ''
    if len(sys.argv) == 2:
        equation = sys.argv[1]
        validate_symbols(equation, symbol='=')

        reduced_form = get_coeficients(equation)
        stringified_reduced_form = stringify_reduced_equation(reduced_form)
        print('Reduced form: {}'.format(stringified_reduced_form))
        powers = [int(power) for power, multiplier in reduced_form.items()]
        if not powers:
            print('The solution is: any rational number')
        elif max(powers) > 2:
            print('Polynomial degree:', max(powers))
            print('The polynomial degree is stricly greater than 2, I can\'t solve.')
        elif not reduced_form.get('2'):
            res = solve_equation(a=reduced_form.get('1', 0.0), c=reduced_form.get('0', 0.0))
            print('Polynomial degree:', 1)
            print('The solution is: {0}'.format(round(res, 4)))
        else:
            res = solve_equation(a=reduced_form.get('2', 0.0), b=reduced_form.get('1', 0.0), c=reduced_form.get('0', 0.0))
            print('Polynomial degree:', 2)
            if isinstance(res, tuple):
                if 'i' in res[0]:
                    print('Discriminant is strictly negative, the two solutions are:\n{0}\n{1}'.format(res[0], res[1]))
                else:
                    print('Discriminant is strictly positive, the two solutions are:\n{0}\n{1}'.format(res[0], res[1]))
            else:
                print('Discriminant is zero, the solution is: {0}'.format(res))

